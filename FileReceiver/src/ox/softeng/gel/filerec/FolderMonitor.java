package ox.softeng.gel.filerec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import ox.softeng.gel.filerec.config.Folder;
import ox.softeng.gel.filerec.config.Header;

import ox.softeng.burst.domain.Severity;

import ox.softeng.burst.services.MessageDTO;
import ox.softeng.burst.services.MessageDTO.Metadata;

public class FolderMonitor implements Runnable {

	String contextPath;
	Path dir;
	String dirName;
	Folder folder;
	
	String queueHost; // "192.168.99.100"
	String exchangeName; // "Carfax"
	String burstQueue; // "noaudit.burst"
	Long refreshTime;
	
	HashMap<String, Long> fileSizes;
	HashMap<String, Long> lastModified;
	HashMap<String, Long> timeDiscovered;

	JAXBContext burstMessageContext;
    
	
	public FolderMonitor(String contextPath, Folder folder, String queueHost, String exchangeName, String burstQueue, Long refreshTime)
	{
		this.contextPath = contextPath;
		dirName = contextPath + "/" + folder.getFolderPath();
		dir = Paths.get(dirName);
		this.folder = folder;
		
		this.queueHost = queueHost; // "192.168.99.100"
		this.exchangeName = exchangeName; // "Carfax"
		this.burstQueue = burstQueue; // "noaudit.burst"
		this.refreshTime = refreshTime;
		fileSizes = new HashMap<String, Long>();
		lastModified = new HashMap<String, Long>();
		timeDiscovered = new HashMap<String, Long>();
		
		try {
			burstMessageContext = JAXBContext.newInstance(MessageDTO.class);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void run() {
		
		
		
		try {
			System.out.println("dir type: " + Files.getFileStore(dir).type());
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		while(true)
		{
			Long currentTime = System.currentTimeMillis();
			// First we'll go through the files that we've already found
			
			// Keep a copy of the keyset so that we can modify the underlying hashset while iterating.
			Set<String> fileNames = new HashSet<String>(timeDiscovered.keySet());
			for(String fileName : fileNames)
			{
				//System.out.println("Re-examining file: " + fileName);
				
				File f = new File(fileName);
				// If the file no-longer exists, then remove it
				if(!f.exists() || f.isDirectory())
				{
					//System.out.println("file no-longer exists!");
					fileSizes.remove(fileName);
					lastModified.remove(fileName);
					timeDiscovered.remove(fileName);
				}
				else{
					Long thisFileLastModified = f.lastModified();
					Long thisFileSize = f.length();
					
					// if it's been modified, then update it
					if(!thisFileLastModified.equals(lastModified.get(fileName))
						|| !thisFileSize.equals(fileSizes.get(fileName)))
					{
						//System.out.println("File modified since last examined.");
						fileSizes.put(fileName,  thisFileSize);
						lastModified.put(fileName, thisFileLastModified);
						timeDiscovered.put(fileName, currentTime);
					
					}
					else // It's the same file as we've seen before...
					{
						// Only consider it if it has been there for a suitable duration
						if(timeDiscovered.get(fileName) + refreshTime < currentTime)
						{
							//System.out.println("File not modified.");
							try {
								handleNewFile(f.getName());
								fileSizes.remove(fileName);
								lastModified.remove(fileName);
								timeDiscovered.remove(fileName);
							} catch (IOException | TimeoutException e) {
								e.printStackTrace();
							} catch (JAXBException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}						
					}
				}
			}

			// Now we'll look and see if there are any new files to add
			for(File f : dir.toFile().listFiles())
			{
				String thisFileAbsolutePath = f.getAbsolutePath();
				// If we've not seen this file before
				if(!lastModified.containsKey(thisFileAbsolutePath))
				{
					Long thisFileLastModified = f.lastModified();
					Long thisFileSize = f.length();
					System.out.println("registering file: " + thisFileAbsolutePath);
					
					fileSizes.put(thisFileAbsolutePath,  thisFileSize);
					lastModified.put(thisFileAbsolutePath, thisFileLastModified);
					timeDiscovered.put(thisFileAbsolutePath, currentTime);
				}
			
			}
			try {
				// Sleep for a second
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}					
			
		} 
        

	}
		
	private void handleNewFile(String filename) throws IOException, TimeoutException, JAXBException
	{
		String fullPath =  contextPath + folder.getFolderPath() + "/" + filename;
		
	    ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(queueHost);
	    Connection connection = factory.newConnection();
	    Channel channel = connection.createChannel();
	    
	    channel.exchangeDeclare(exchangeName, "topic", true);
	    byte[] message = Files.readAllBytes(Paths.get(fullPath));
	    
	    Map<String,Object> headerMap = new HashMap<String, Object>();
	    headerMap.put("filename",filename);
	    headerMap.put("directory",contextPath + folder.getFolderPath());
	    headerMap.put("receivedDateTime",OffsetDateTime.now(ZoneId.systemDefault()).toString());
	    for(Header h : folder.getHeaders().getHeader())
	    {
	    	headerMap.put(h.getKey(), h.getValue());
	    }
	    AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
	    builder.headers(headerMap);
	    
	    channel.basicPublish(exchangeName, folder.getQueueName(), builder.build(), message);
	    //String burstMessage = "File received";

	    
	    channel.basicPublish(exchangeName, burstQueue, builder.build(), getSuccessMessage(filename));
	    //System.out.println(" [x] Sent '" + message + "'");
		
	    String newPath = contextPath + folder.getMoveDestination() + "/" + filename;
		File file = new File(fullPath);
		file.renameTo(new File(newPath));
		channel.close();
		connection.close();
	    
		System.out.println("Processed: " + fullPath);
	}
	
	
	private byte[] getSuccessMessage(String filename) throws JAXBException
	{
	    MessageDTO burstMessage = new MessageDTO();
	    burstMessage.setDateTimeCreated(LocalDateTime.now());
	    burstMessage.setSeverity(Severity.NOTICE);
	    burstMessage.setSource("Sample tracking system");
	    String GMCName = "Unknown GMC";
	    for(Header h : folder.getHeaders().getHeader())
	    {
	    	if("GMC".equalsIgnoreCase(h.getKey()))
	    	{
	    		GMCName = h.getValue();
	    		break;
	    	}
	    }
	    burstMessage.setDetails("A file with the name \"" + filename + "\" has been uploaded for sample tracking by " + GMCName);
	    burstMessage.addTopic("File Receipt");
	    burstMessage.addMetadata(new Metadata("GMC", GMCName));
	    burstMessage.addMetadata(new Metadata("File name", filename));
	    
	    Marshaller m = burstMessageContext.createMarshaller(); 
	    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	    ByteArrayOutputStream bas = new ByteArrayOutputStream();
	    m.marshal(burstMessage, bas );
	    return bas.toByteArray();
	}

}
