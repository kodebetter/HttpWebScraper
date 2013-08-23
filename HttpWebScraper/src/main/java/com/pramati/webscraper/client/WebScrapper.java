/**
 * 
 */
package com.pramati.webscraper.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.pramati.webscraper.client.impl.Request;
import com.pramati.webscraper.client.impl.Response;
import com.pramati.webscraper.executors.ThreadExecutor;
import com.pramati.webscraper.utils.HTMLLinkExtractor;
import com.pramati.webscraper.utils.HtmlLink;

/**
 * @author sandeep-t
 * Util Class for doing operations like url reading, url extraction and data extraction.
 *   
 */

public class WebScrapper {

	private static final Logger LOGGER = Logger.getLogger(WebScrapper.class);
	private static final  int BUFFERSIZE = 1024;
	private static final String FILESEPARATOR=System.getProperty("file.separator");
	//private static ExecutorService executor = Executors.newFixedThreadPool(20);
	//private static List<Future<Response>> childFutureList = Collections.synchronizedList(new ArrayList<Future<Response>>());
	//private static Vector<Future<Response>> childFutureList = new Vector<Future<Response>>();
	public static BlockingQueue<Future<Response>> childFutureList = new LinkedBlockingQueue<Future<Response>>();
	
/**
 * This method will take html data in form of String and will 
 * return a list of weblinks embedded in thathtml data 
 * @param htmlData
 * @return list of webaddress
 */
	public void getWebLinksListFromHtml(String htmlData,String webLink) {
		
		htmlData.replaceAll("\\s+", " ");
		final HTMLLinkExtractor extractor = new HTMLLinkExtractor();
		final List<HtmlLink> links = extractor.grabHTMLLinks(htmlData);
		StringBuilder stbr;
		URL url;
		Request task;
		for (HtmlLink link:links) {
			stbr = new StringBuilder();
			try {
				stbr.append(webLink).append("/")
						.append(link.getLink());
				url = new URL(stbr.toString());
				task = new Request(url);
				final Future<Response> response =ThreadExecutor.getInstance().submitTask(task);
				childFutureList.add(response);
			} catch (MalformedURLException e1) {
				LOGGER.error(
						"MalformedURLException occured while parsing the URL "
								+ stbr.toString(), e1);
				
			}			
		
		}
	}

	/**
	 * This function serves the purpose of hitting the the given 
	 * URL and get the response out of it.
	 * If in case any exception occurs the exception
	 * will be thrown up and the system is going to halt.
	 * 
	 * @param urlOfMainPage
	 * @return Future as the response
	 * @throws Exception
	 */
	public Future<Response> getMainPageResponse(final String urlString) throws Exception {
		try {
			final URL url = new URL(urlString);
			Request task = new Request(url);
			return ThreadExecutor.getInstance().submitTask(task);
		} catch (MalformedURLException e1) {
			LOGGER.error("MalformedURLException occured while parsing the URL "
					+ urlString, e1);
			throw new Exception(
					"MalformedURLException occured while parsing the URL "
							+ urlString, e1);
		}
	}

	/**
	 * 
	 * This fuction will be used to get the html response after parsing the
	 * data from future passed to the function. 
	 * @param responseList
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	public String readResponseForHtmlData(final Future<Response> responseList)
			throws InterruptedException, ExecutionException, IOException {
		final Response resp = responseList.get();
		final InputStream body = resp.getBody();
		final BufferedReader responseBuffer = new BufferedReader(new InputStreamReader(
				body));
		String output;
		StringBuilder fileContent = new StringBuilder();
		//System.out.println("Output from Server .... \n");
		try {
			while ((output = responseBuffer.readLine()) != null) {
				fileContent.append(output);
			}
		} catch (IOException e) {
			LOGGER.error("Exception occured while reading data from Response",
					e);
			throw new IOException(
					"Exception occured while reading data from Response", e);
			
		} finally {
			responseBuffer.close();
		}
	
		return fileContent.toString();
	}

	/**
	 * This function will take the two arguments as the input and will 
	 * return the list of Future for all the links in the urlLists.  
	 * @param urlLists - list of URLs extracted from htmlData.  
	 * @param webLink-the weblink that was hit initially.
	 * @return
	 */
	public List<Future<Response>> getResonseListForWebLinks(final List<String> urlLists,String webLink) {
		final List<Future<Response>> childFutureList = new ArrayList<Future<Response>>();
		StringBuilder stbr;
		URL url;
		Request task;
		for (final String hyperLink : urlLists) {
			
			stbr= new StringBuilder();
			try {
				
				stbr.append(webLink).append(FILESEPARATOR).append(hyperLink);
			
				//System.out.println();
				url = new URL(stbr.toString());
				task = new Request(url);
				final Future<Response> response = ThreadExecutor.getInstance().submitTask(task);
				childFutureList.add(response);
			} catch (MalformedURLException e1) {
				LOGGER.error(
						"MalformedURLException occured while parsing the URL "
								+ stbr.toString(), e1);
				// throw new
				// Exception("MalformedURLException occured while parsing the URL "+spec,
				// e1);
			} 
			
		}
		
		return childFutureList;
	}

	

/**
 * This method will be used for writing the extracted data in form of
 * input stream into the files
 * 
 * @param in
 * @param outFileStr
 * @throws IOException 
 */
	public void writeStreamToFile(InputStream in, String outFileStr) throws IOException {
		
		try {
			final FileOutputStream out = new FileOutputStream(outFileStr);
			int numBytesRead;
			final byte[] byteBuf = new byte[BUFFERSIZE];
			while ((numBytesRead = in.read(byteBuf)) != -1) {
				out.write(byteBuf, 0, numBytesRead);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error("Exception occurecd while writing file " + outFileStr);
			throw new FileNotFoundException("Exception occurecd while writing file " + outFileStr);
		} catch (IOException e) {
			LOGGER.error("IOException occurecd while writing file " + outFileStr);
			throw new IOException("IOException occurecd while writing file " + outFileStr);
		}

	}
	
	
}
