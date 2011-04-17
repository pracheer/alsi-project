package ssm;

/**
 * @author @bby
 *
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;

class DataStorage{
	int w, wq, r;
	public int GetNumberOfServersToSendWriteRequest(){return w;}
	public void SetNumberOfServersToSendWriteRequest(int w){this.w = w;}
	public int GetNumberOfServersToWaitAfterWriteRequest(){return wq;}
	public void SetNumberOfServersToWaitAfterWriteRequest(int wq){this.wq = wq;}
	public int GetNumberOfServersToSendReadRequest(){return r;}
	public void SetNumberOfServersToSendReadRequest(int r){this.r = r;}
}


public class SimpleDBInterface {
	
	private static final String 
	ITEM = "Item_",
	PORT = "Port",
	NAME = "Hostname",
	WRITE_REQ = "NoOfServersToSendWriteRequest",
	WRITE_RSP = "NoOfServersToWaitForWriteResponse",
	READ_REQ = "NoOfServersToSendReadRequest";
	
	AmazonSimpleDB sdb;
	static SimpleDBInterface instance = new SimpleDBInterface();
	String domainName = "CS5300Proj1ServerNames";
	String dataStorageDomainName = "CS5300Proj1StoragePlaces";
	private static final String 
	defaultWritesRequestsSend = "3", 
	defaultWritesAcknowledgementsNeeded = "2", 
	defaultReadRequests="2";
	//int serial = 0;
	
	SimpleDBInterface()
	{
		try {
			sdb = new AmazonSimpleDBClient(new PropertiesCredentials(
					SimpleDBInterface.class.getResourceAsStream("AwsCredentials.properties")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		boolean domainExists = false, dataStorageDomainExists = false;
		for (String domainNameE : sdb.listDomains().getDomainNames()) {
            if(domainNameE.equals(domainName))
            	domainExists = true;
            if(domainNameE.equals(dataStorageDomainName))
            	dataStorageDomainExists = true;
            if(domainExists && dataStorageDomainExists)
            	break;
        }
		if(!domainExists)
		{
			System.out.println("Creating domain called " + domainName + ".\n");
			sdb.createDomain(new CreateDomainRequest(domainName));
		}
		if(!dataStorageDomainExists)
		{
			System.out.println("Creating domain called " + dataStorageDomainName + ".\n");
			sdb.createDomain(new CreateDomainRequest(dataStorageDomainName));
		}
		String selectExpression = "select * from `" + dataStorageDomainName + "`";
        //System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        List<Item> items = sdb.select(selectRequest).getItems();
        if(items.size() == 0)
        {
        	List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
			sampleData.add(
					new ReplaceableItem(ITEM).withAttributes(
	                    new ReplaceableAttribute(WRITE_REQ, defaultWritesRequestsSend, true),
	                    new ReplaceableAttribute(WRITE_RSP, defaultWritesAcknowledgementsNeeded, true),
	                    new ReplaceableAttribute(READ_REQ, defaultReadRequests, true)
	                    ));

    		sdb.batchPutAttributes(new BatchPutAttributesRequest(dataStorageDomainName, sampleData));
        }
	}
	
	public DataStorage getDataStoragePattern()
	{
		String selectExpression = "select * from `" + dataStorageDomainName + "`";
        //System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        List<Item> items = sdb.select(selectRequest).getItems();
        DataStorage ds = new DataStorage();
        if(items.size()>1)
        	System.err.println("Some Big error");
        for (Attribute attribute : items.get(0).getAttributes()) {
        	String name = attribute.getName();
            if(name.equals(WRITE_REQ)) 
            	ds.SetNumberOfServersToSendWriteRequest(Integer.parseInt(attribute.getValue()));
            else
            {
            	if(name.equals(WRITE_RSP)) 
                	ds.SetNumberOfServersToWaitAfterWriteRequest(Integer.parseInt(attribute.getValue()));
            	else
            		if(name.equals(READ_REQ))
            			ds.SetNumberOfServersToSendReadRequest(Integer.parseInt(attribute.getValue()));
            }
        }
        return ds;
	}
	
	public void updatetaStoragePattern(DataStorage ds)
	{
		List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
        replaceableAttributes.add(new ReplaceableAttribute(WRITE_REQ, ds.GetNumberOfServersToSendWriteRequest()+"", true));
        replaceableAttributes.add(new ReplaceableAttribute(WRITE_RSP, ds.GetNumberOfServersToWaitAfterWriteRequest()+"", true));
        replaceableAttributes.add(new ReplaceableAttribute(READ_REQ, ds.GetNumberOfServersToSendReadRequest()+"", true));
        sdb.putAttributes(new PutAttributesRequest(dataStorageDomainName, ITEM, replaceableAttributes));
	}
	
	public Item getMember(String ip, int port)
	{ 
		String selectExpression = "select * from `" + domainName + "` where Hostname = \'"+ ip +"\' AND Port = \'" + port +"\'";
        System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        List<Item> items = sdb.select(selectRequest).getItems();
        if(items.size()>0)
        	return items.get(0);
        else
        	return null;
	}
	
	public Members getMembers()
	{ 
		String selectExpression = "select * from `" + domainName + "`";
        //System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        List<Item> items = sdb.select(selectRequest).getItems();
        Members members = new Members();
        for (Item item : items) {
        	String ip = "";
        	int port = -1;
			List<Attribute> attributes = item.getAttributes();
			for (Attribute attribute : attributes) {
				if(attribute.getName().equalsIgnoreCase(NAME))
					ip = attribute.getValue();
				else if(attribute.getName().equalsIgnoreCase(PORT))
					port = Integer.parseInt(attribute.getValue());
			}
			Member member = new Member(ip, port);
			members.add(member);
		}
        return members;
	}
	
	public void displayMembers()
	{ 
		String selectExpression = "select * from `" + domainName + "`";
        //System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        List<Item> items = sdb.select(selectRequest).getItems();
        for (Item item : items) {
            System.out.println("  Item");
            System.out.println("    Name: " + item.getName());
            for (Attribute attribute : item.getAttributes()) {
                System.out.println("      Attribute");
                System.out.println("        Name:  " + attribute.getName());
                System.out.println("        Value: " + attribute.getValue());
            }
        }
	}
	
	public boolean addMember(String ip, int port)
	{
		System.out.println(".\n.\n.Adding "+ ip+ " "+ port);
		//serial++;
		List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
		boolean added = false;
        if(getMember(ip, port)==null)
        {
        	sampleData.add(new ReplaceableItem(ITEM + ip + "_" + port).withAttributes(
                    new ReplaceableAttribute(NAME, ip, false),
                    new ReplaceableAttribute(PORT, port+"", false)));

    		sdb.batchPutAttributes(new BatchPutAttributesRequest(domainName, sampleData));
    		added = true;
        }
        displayMembers();
        return added;
	}
	
	public void cleanAll()
	{
		String selectExpression = "select * from `" + domainName + "`";
        //System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        List<Item> items = sdb.select(selectRequest).getItems();
        for (Item item : items) {
        	 sdb.deleteAttributes(new DeleteAttributesRequest(domainName, item.getName()));
        }
	}

/*	public boolean addAll(Members members)
	{
		serial++;
		List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
		boolean added = false;
        if(getMember(name, port)!=null)
        {
        	for (Member member : members) {
        		sampleData.add(new ReplaceableItem("Item_" + serial).withAttributes(
        				new ReplaceableAttribute("Name", member.getIpAddress(), false),
        				new ReplaceableAttribute("Port", port, false)));
			}

    		sdb.batchPutAttributes(new BatchPutAttributesRequest(domainName, sampleData));
    		added = true;
        }
        return added;
	}
*/
	public boolean removeMember(String ip, int port)
	{
		//serial++;
		Item item = getMember(ip, port);
		boolean removed = false;
        if( item !=null)
        {
        	 //System.out.println("Deleting item" + item.getName());
        	 sdb.deleteAttributes(new DeleteAttributesRequest(domainName, item.getName()));
        	 
        	 removed = true;
        }
        return removed;
	}
	
	public static SimpleDBInterface getInstance()
	{
		return instance;
	}
	
}
