package com.imaginarymachines.vosao.deliciousimport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.impl.FeedFetcherCache;
import org.rometools.fetcher.impl.HashMapFeedInfoCache;
import org.rometools.fetcher.impl.HttpURLFeedFetcher;
import org.vosao.business.mq.MessageQueue;
import org.vosao.business.mq.Topic;
import org.vosao.business.mq.message.ExportMessage;
import org.vosao.business.mq.message.PageMessage;
import org.vosao.business.plugin.PluginCronJob;
import org.vosao.common.VosaoContext;
import org.vosao.entity.PageEntity;
import org.vosao.entity.StructureEntity;
import org.vosao.entity.StructureTemplateEntity;
import org.vosao.entity.TagEntity;
import org.vosao.entity.TemplateEntity;
import org.vosao.enums.PageState;
import org.vosao.enums.PageType;
import org.vosao.business.page.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * Possible cron parameters:
 * 
 *  every day
 *  every monday
 *  every NN day of month
 * 
 */
public class DeliciousimportJob implements PluginCronJob {

        private static final Log logger = LogFactory.getLog(DeliciousimportJob.class);

        private DeliciousimportConfig config;
        
        public DeliciousimportJob(DeliciousimportConfig config) {
                this.config = config;
        }
        
        
        @Override
        public void run() {
        	//getMessageQueue().publish(new PageMessage(Topic.PAGE_PUBLISH_CRON, "message"));
        	
        	SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        	Date now = new Date();
        	
        	int daysback = Integer.parseInt(config.getRecency());
        	logger.info("DeliciousImport Recency = "+daysback+" days");
        	
        	try { 
	        	HttpClient httpclient = new HttpClient();
	    		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				
	        	FeedFetcher feedFetcher = new HttpURLFeedFetcher();
	            //SyndFeed feed = feedFetcher.retrieveFeed(new URL("http://feeds.delicious.com/v2/rss/npohle?count=100"));
	        	SyndFeed feed = feedFetcher.retrieveFeed(new URL(config.getDeliciousrssfeed()));
	            List<SyndEntry> entries = feed.getEntries();
	            
	        	for (int i=0; i<=daysback; i++) {
	        		Date day = new Date(now.getTime()-(i*1000*60*60*24));
	        		
		        	try {
			        	
			        	String title = "Digest from "+formatter.format(day);
			        	
			        	StringBuffer overview = new StringBuffer("<ul>");
	
			        	HashSet<SyndCategory> cats = new HashSet<SyndCategory>();
	
			        	try {
			        		
				            Iterator<SyndEntry> iterEntries = entries.iterator();
				            while (iterEntries.hasNext()) {
				            	SyndEntry entry = iterEntries.next();
				            	Date pubDate = entry.getPublishedDate();
				            	if (day.getDate()==pubDate.getDate() && day.getMonth()==pubDate.getMonth() && day.getYear()==pubDate.getYear()) {
				            		
				            		cats.addAll(entry.getCategories());
				            		
				            		StringBuffer responseXml = new StringBuffer();
				            		
				            		try {
				                        URL url = new URL("http://api.bit.ly/shorten"
				                        		+"?longUrl="+URLEncoder.encode(entry.getLink())
				                        		+"&version="+URLEncoder.encode("2.0.1")
				                        		+"&login="+URLEncoder.encode("npohle")
				                        		+"&apiKey="+URLEncoder.encode(config.getBitlyapikey())
				                        		+"&format="+URLEncoder.encode("xml")
				                        		+"&history="+URLEncoder.encode("1"));
				                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
				                        String line;
				                        while ((line = reader.readLine()) != null) {
				                        	responseXml.append(line);
				                        }
				                        reader.close();
				                        
				                    } catch (Exception e) {
				                        e.printStackTrace();
				                    }
				            		
				            		String bitlylink = entry.getLink();
				            		if(responseXml != null) {
				            			StringReader st = new StringReader(responseXml.toString());
				            			Document d = db.parse(new InputSource(st));
				            			NodeList nl = d.getElementsByTagName("shortUrl");
				            			if(nl != null) {
				            				Node n = nl.item(0);
				            				bitlylink = n.getTextContent();
				            			}
				            		}
				            		
				            		String desciption="";
				            		if (entry.getDescription()!=null) desciption=entry.getDescription().getValue();
				            		
				            		overview.append("<li><a href=\""+bitlylink+"\">"+entry.getTitle()+"</a> <i>"+desciption+"</i></li>");
				            		
				            	}
				            }
			        	} catch(Exception e) {
			        		e.printStackTrace();
			        	}
			        	overview.append("</ul>");
			        	
			        	if (overview.length()>10) {
			        	
			        		logger.info("DelicousImport: Found content for "+day.toString());
			        		
				        	StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" encoding=\"utf-8\"?><content>");
			        		xml.append("<overview><![CDATA["+overview+"]]></overview>");
			                xml.append("<content><![CDATA[]]></content>");	        	
				        	xml.append("</content>");
				        	
				        	int versionidx = 0;
				        	List<PageEntity> versions = VosaoContext.getInstance().getBusiness().getDao().getPageDao().selectByUrl("/blog/"+title);
				        	if (versions.size()>0) {
				        			for (PageEntity version : versions) {
				        				if (version.getVersion()>versionidx) versionidx=version.getVersion();
				        			}
				        	}
				        	versionidx += 1;
				        	
				        	PageEntity page = new PageEntity(title,"/blog/"+title);
				        	page.setParentUrl("/blog");
				        	page.setVersion(versionidx);
				        	page.setVersionTitle("Version "+versionidx+" ("+formatter.format(now)+")");
				        	page.setPublishDate(day);
				        	page.setState(PageState.APPROVED);
				        	
				        	StructureTemplateEntity structureTemplate = VosaoContext.getInstance().getBusiness().getDao().getStructureTemplateDao().getByTitle("Article");
				        	StructureEntity structure = VosaoContext.getInstance().getBusiness().getDao().getStructureDao().getByTitle("Blog article");
				        	TemplateEntity template = VosaoContext.getInstance().getBusiness().getDao().getTemplateDao().getByUrl("coolblue10");
				        	
				        	page.setPageType(PageType.STRUCTURED);
				        	page.setStructureId(structure.getId());
				        	page.setStructureTemplateId(structureTemplate.getId());
				        	page.setTemplate(template.getId());
				        	VosaoContext.getInstance().getBusiness().getPageBusiness().save(page);
				        	
				        	VosaoContext.getInstance().getBusiness().getDao().getPageDao().setContent(page.getId(), "en", xml.toString());
				        	VosaoContext.getInstance().getBusiness().getPageBusiness().save(page);
				        	
			        		Iterator<SyndCategory> iterCats = cats.iterator();
			        		while (iterCats.hasNext()) {
			        			SyndCategory cat = iterCats.next();
			        			TagEntity tag = VosaoContext.getInstance().getBusiness().getDao().getTagDao().getByName(null, cat.getName());
			        			if (tag==null) {
			        				tag = VosaoContext.getInstance().getBusiness().getDao().getTagDao().save(new TagEntity(null, cat.getName(), cat.getName()));
			        			}
			        			VosaoContext.getInstance().getBusiness().getTagBusiness().addTag(page.getFriendlyURL(), tag);
			        		}
		        		
			        	}
			        	
			        	//VosaoContext.getInstance().getBusiness().getDao().getPageDao().setContent(page.getId(), "en", "Bla Bla Bla");
			        	
			        	
						
		        	} catch(Exception e) {
		        		logger.fatal("Error in DeliciousimportJob.java: "+e.getMessage(),e);
		        		e.printStackTrace();
		        	}
	        	}
        	} catch (Exception ee) {
        		logger.fatal("Error in DeliciousimportJob.java: "+ee.getMessage(),ee);
        		ee.printStackTrace();
        	}
        }

        @Override
        public boolean isShowTime(Date date) {
        		String cron = config.getCron();
                try {
                        Calendar cal = Calendar.getInstance();
                        String[] items = cron.split(" ");
                        if (cron.equals("every minute")) {return true;}
                        if (cron.equals("every hour")) {
                            return cal.get(Calendar.MINUTE) == 0;
                        }
                        if (cron.equals("every day")) {
                                // start at 01:00
                                return cal.get(Calendar.HOUR_OF_DAY) == 1 
                                        && cal.get(Calendar.MINUTE) == 0;
                        }
                        if (cron.endsWith("day of month")) {
                                int day = Integer.valueOf(items[1]);
                                // start at 01:00
                                return cal.get(Calendar.DAY_OF_MONTH) == day
                                && cal.get(Calendar.HOUR_OF_DAY) == 1 
                                && cal.get(Calendar.MINUTE) == 0;
                        }
                        int weekDay = getWeekday(items[1]);
                        return cal.get(Calendar.DAY_OF_WEEK) == weekDay
                        && cal.get(Calendar.HOUR_OF_DAY) == 1 
                        && cal.get(Calendar.MINUTE) == 0;
                }
                catch (Exception e) {
                        logger.error("Error during parsing backup cron expression. \"" + cron + "\": "+ e.getMessage()+", StackTrace: "+e.getStackTrace().toString());
                        return false;
                }
                
        }

        private int getWeekday(String day) {
                if (day.equals("monday")) {
                        return Calendar.MONDAY;
                }
                if (day.equals("tuesday")) {
                        return Calendar.TUESDAY;
                }
                if (day.equals("thursday")) {
                        return Calendar.THURSDAY;
                }
                if (day.equals("wednesday")) {
                        return Calendar.WEDNESDAY;
                }
                if (day.equals("friday")) {
                        return Calendar.FRIDAY;
                }
                if (day.equals("saturday")) {
                        return Calendar.SATURDAY;
                }
                if (day.equals("sunday")) {
                        return Calendar.SUNDAY;
                }
                return Calendar.MONDAY;
        }

}