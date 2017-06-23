import java.util.Scanner;
import java.util.Vector;
import java.net.*;
import java.io.*;

public class menuReader
{
	public static final String nl = "nl";

	public static boolean containsMinusCase(String s1, String s2)
	{
		int sLen = s2.length();
		int maxLen = s1.length() - sLen;
		
		char loFirst = Character.toLowerCase(s2.charAt(0));
		char upFirst = Character.toUpperCase(s2.charAt(0));
		
		for(int i = 0; i <= maxLen; i++)
		{
			if(s1.charAt(i) != loFirst && s1.charAt(i) != upFirst)
				continue;
			
			if(s1.regionMatches(true, i, s2, 0, sLen))
				return true;	
		}
		return false;
	}
	
	public static void openFile(Vector<String> list, String fileName)
	{
		try
		{
			Scanner scan = new Scanner(new File(fileName));
			while(scan.hasNext())
				list.add(scan.nextLine());
			scan.close();
			list.trimToSize();
		} catch(IOException e) {}
	}
	
	public static boolean vectorContains(Vector<String> list, String food)
	{
		for(int i = 0; i < list.size(); i++)
			if(containsMinusCase(food, list.elementAt(i)))
				return true;
		return false;
	}
	
	public static void main( String[] args )
	{
		Vector<String> wants = new Vector<String>();
		Vector<String> dislikes = new Vector<String>();
		Vector<String> allergies = new Vector<String>();
		boolean vegetarian = false;
		boolean vegan = false;
		
		openFile(wants, "Wants.txt");
		openFile(dislikes, "Dislikes.txt");
		openFile(allergies, "Allergy.txt");
		
		if(wants.isEmpty() && dislikes.isEmpty() && allergies.isEmpty() && !vegetarian && !vegan)
		{
			System.out.println("Everything was empty");
			return;
		}
		
		HttpURLConnection connection = null;
		
		try
		{
			URL url = new URL("https://nccudining.sodexomyway.com/dining-choices/index.html");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setInstanceFollowRedirects(true);
			connection.connect();

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			Vector<String> menusAndDates = new Vector<String>(4);
			String inputLine;
			
			while((inputLine = in.readLine()) != null)
			{
				if(inputLine.contains("On the Menu"))
				{
					for(int i = 0; i < 10; i++)
					{
						if((inputLine = in.readLine()).contains("images"))
						{
							String menu = "https://nccudining.sodexomyway.com/" + inputLine.substring(62, inputLine.length() - 18);
							String date = in.readLine();
							date = date.substring(48, date.length());
							menusAndDates.add(menu);
							menusAndDates.add(date);
						}
					}
					break;
				}
			}
			in.close();
			
			URL newURL;
			
			if(menusAndDates.size() > 2)
			{
				Scanner scan = new Scanner(System.in);
				System.out.println("There are two menus.");
				System.out.println("Enter 0 to read this week's menu or 1 to read next week's menu");
				int choice = (2 * scan.nextInt()) + 1;
				newURL = new URL(menusAndDates.elementAt(choice));
				scan.close();
			}
			else
			{
				System.out.println("There is one menu.");
				newURL = new URL(menusAndDates.elementAt(0));
			}
			
			in = new BufferedReader(new InputStreamReader(newURL.openStream()));
			
			for(int i = 0; i < 1400; i++)
				in.readLine();
			
			Vector<String> output = new Vector<String>();
			
			while((inputLine = in.readLine()) != null)
			{
				if(inputLine.equals("<!-- END DAY DATA -->"))
					break;

				if(inputLine.startsWith("<!--"))
				{
					String temp = inputLine.substring(5, inputLine.length() - 4);
					temp = temp.substring(0, 1) + temp.substring(1, temp.length()).toLowerCase();
					
					if(temp.charAt(0) != 'M')
						output.add(nl);

					output.add(temp);
					output.add(nl);
					continue;
				}
				
				if(inputLine.contains("BREAKFAST") || inputLine.contains("LUNCH") || inputLine.contains("DINNER"))
				{
					inputLine = inputLine.substring(inputLine.indexOf("mealname") + 10, inputLine.indexOf("</td>"));
					inputLine = inputLine.substring(0, 1) + inputLine.substring(1, inputLine.length()).toLowerCase() + ":";
					
					if(inputLine.charAt(0) != 'B')
						output.add(nl);
					
					output.add(inputLine);
					output.add(nl);
					continue;
				}
				
				if(inputLine.startsWith("   <span class"))
				{
					if(vegan)
						if(!inputLine.contains("Vegan"))
							continue;
					
					if(vegetarian)
						if(!(inputLine.contains("Vegan") || inputLine.contains("Vegetarian")))
							continue;
					
					inputLine = inputLine.substring(inputLine.indexOf("pcls(this);") + 13, inputLine.indexOf("</span>"));
					
					if(!wants.isEmpty())
						if(!vectorContains(wants, inputLine))
							continue;
					
					if(!dislikes.isEmpty())
						if(vectorContains(dislikes, inputLine))
							continue;
					
					output.add(inputLine);
					output.add(nl);
				}
			}
			output.trimToSize();
			
			if(!allergies.isEmpty())
			{
				while((inputLine = in.readLine()) != null)
				{
					if(inputLine.equals("</script>"))
						break;
					
					if(!inputLine.startsWith("aData["))
						continue;
					
					for(int i = 0; i < output.size(); i++)
					{
						if(output.elementAt(i).equals(nl) || output.elementAt(i).contains("day"))
							continue;
						if(output.elementAt(i).equals("Breakfast") || output.elementAt(i).equals("Lunch") || output.elementAt(i).equals("Dinner"))
							continue;
						
						if(inputLine.contains(output.elementAt(i)))
						{
							for(int j = 0; j < allergies.size(); j++)
							{
								if(containsMinusCase(inputLine, allergies.elementAt(j)))
								{
									output.remove(i);
									output.remove(i);
									break;
								}
							}
						}
					}
				}
				output.trimToSize();
			}
			in.close();
			
			File file = new File("Sodexo.txt");
			file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			
			for(int i = 0; i < output.size(); i++)
			{
				if(output.elementAt(i).equals(nl))
					out.newLine();
				else
					out.write(output.elementAt(i));
			}
			out.close();
		} catch ( MalformedURLException e1 )
		{}
		catch(IOException e)
		{ System.out.println("Could not connect."); }
		finally {connection.disconnect();}
	}
}
