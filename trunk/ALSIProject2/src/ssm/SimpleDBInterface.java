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
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;


public class SimpleDBInterface {
	
	private static final String ITEM = "Item_";
	private static final String PORT = "Port";
	private static final String NAME = "Hostname";
	AmazonSimpleDB sdb;
	static SimpleDBInterface instance = new SimpleDBInterface();
	String domainName = "CS5300Proj1AbhiHim";
	int serial = 0;
	
	SimpleDBInterface()
	{
		try {
			sdb = new AmazonSimpleDBClient(new PropertiesCredentials(
					SimpleDBInterface.class.getResourceAsStream("AwsCredentials.properties")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		boolean domainExists = false;
		for (String domainNameE : sdb.listDomains().getDomainNames()) {
            if(domainNameE.equals(domainName))
            {
            	domainExists = true;
            	break;
            }
        }
		if(!domainExists)
		{
			System.out.println("Creating domain called " + domainName + ".\n");
			sdb.createDomain(new CreateDomainRequest(domainName));
		}
	}
	
	public Item getMember(String ip, int port)
	{ 
		String selectExpression = "select * from `" + domainName + "` where Name = \'"+ ip +"\' AND Port = \'" + port +"\'";
        System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        for (Item item : sdb.select(selectRequest).getItems()) {
            System.out.println("  Item");
            System.out.println("    Name: " + item.getName());
            for (Attribute attribute : item.getAttributes()) {
                System.out.println("      Attribute");
                System.out.println("        Name:  " + attribute.getName());
                System.out.println("        Value: " + attribute.getValue());
            }
        }
        List<Item> items = sdb.select(selectRequest).getItems();
        if(items.size()>0)
        	return items.get(0);
        else
        	return null;
	}
	
	public Members getMembers()
	{ 
		String selectExpression = "select * from `" + domainName + "`";
        System.out.println("Selecting: " + selectExpression + "\n");
        SelectRequest selectRequest = new SelectRequest(selectExpression);
        for (Item item : sdb.select(selectRequest).getItems()) {
            System.out.println("  Item");
            System.out.println("    Name: " + item.getName());
            for (Attribute attribute : item.getAttributes()) {
                System.out.println("      Attribute");
                System.out.println("        Name:  " + attribute.getName());
                System.out.println("        Value: " + attribute.getValue());
            }
        }
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
	
	public boolean addMember(String ip, int port)
	{
		serial++;
		List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
		boolean added = false;
        if(getMember(ip, port)==null)
        {
        	sampleData.add(new ReplaceableItem(ITEM + serial).withAttributes(
                    new ReplaceableAttribute(NAME, ip, false),
                    new ReplaceableAttribute(PORT, port+"", false)));

    		sdb.batchPutAttributes(new BatchPutAttributesRequest(domainName, sampleData));
    		added = true;
        }
        return added;
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
		serial++;
		List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
		Item item = getMember(ip, port);
		boolean removed = false;
        if( item !=null)
        {
        	sampleData.add(new ReplaceableItem(ITEM + serial).withAttributes(
                    new ReplaceableAttribute(NAME, ip, false),
                    new ReplaceableAttribute(PORT, port+"", false)));

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
