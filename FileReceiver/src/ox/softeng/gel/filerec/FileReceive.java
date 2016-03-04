package ox.softeng.gel.filerec;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import ox.softeng.gel.filerec.config.Config;
import ox.softeng.gel.filerec.config.Context;
import ox.softeng.gel.filerec.config.Folder;

public class FileReceive {

	public static void main(String[] args) throws JAXBException, IOException, InterruptedException {
		if(args.length < 5)
		{
			System.err.println("Usage: FileReceive configFile queueHost exchangeName burstQueue refreshTime(s)");
			System.exit(0);
		}
		String configFilename = args[0];
		String queueHost = args[1];
		String exchangeName = args[2];
		String burstQueue = args[3];
		Long refreshTime = Long.parseLong(args[4]);
		System.err.println("Using config file: " + configFilename);
		System.err.println("Using Queue Host: " + queueHost);
		System.err.println("Using Exchange Name: " + exchangeName);
		System.err.println("Using BuRST Queue: " + burstQueue);
		System.err.println("Using Refresh Time: " + refreshTime + " seconds");
		refreshTime = refreshTime * 1000; // Convert to ms

		JAXBContext jc = JAXBContext.newInstance(Config.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();
        File xml = new File(configFilename);
        Config config = (Config) unmarshaller.unmarshal(xml);
        
        for(Context c : config.getContext())
        {
        	for(Folder f : c.getFolder())
        	{
        		FolderMonitor fm = new FolderMonitor(c.getPath(),f, queueHost, exchangeName, burstQueue, refreshTime);
        		//Touch tm = new Touch(Paths.get(c.getPath() + "/" + f.getFolderPath()));
        		new Thread(fm).start();
        		//new Thread(tm).start();
        		
        		System.out.println("Started process for folder " + c.getPath() + "/" + f.getFolderPath());
        	}
        }

		

	}

}
