/*
 * ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 * if any, must include the following acknowledgment:
 * "This product includes software developed by the
 * Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowledgment may appear in the software itself,
 * if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 * "Apache JMeter" must not be used to endorse or promote products
 * derived from this software without prior written permission. For
 * written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 * "Apache JMeter", nor may "Apache" appear in their name, without
 * prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.jmeter.protocol.http.sampler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.apache.jorphan.io.TextFile;

import org.apache.soap.util.xml.*;
import org.apache.jmeter.gui.JMeterFileFilter;
import org.apache.jmeter.protocol.http.util.DOMPool;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.soap.*;
import org.apache.soap.messaging.*;
import org.apache.soap.transport.*;
import javax.xml.parsers.*;
import org.w3c.dom.Document;
import org.xml.sax.*;

/**
 * Title:		JMeter Access Log utilities<br>
 * Copyright:	Apache.org<br>
 * Company:		nobody<br>
 * License:<br>
 * <br>
 * Look at the apache license at the top.<br>
 * <br>
 * Description:<br>
 * Sampler to handle Web Service requests. It uses Apache
 * soap drivers to perform the XML generation, connection,
 * soap encoding and other soap functions.
 * <br>
 * Author:	Peter Lin<br>
 * Version: 	0.1<br>
 * Created on:	Jun 26, 2003<br>
 * 
 * @author Peter Lin
 * @version $Id: 
 */
public class WebServiceSampler extends HTTPSampler
{
	public static final String XML_DATA = "HTTPSamper.xml_data";
	public static final String SOAP_ACTION = "Soap.Action";
	public static final String XML_DATA_FILE = "WebServiceSampler.xml_data_file";
	public static final String XML_PATH_LOC = "WebServiceSampler.xml_path_loc";
	public static final String MEMORY_CACHE = "WebServiceSampler.memory_cache";
	public static final String READ_RESPONSE = "WebServiceSampler.read_response";
	/**
	 * The SOAPAction is required by MS
	 * webservices and is defined by the
	 * WSDL.
	 */
	protected String SOAPACTION = null;
	/**
	 * SampleResult which holds the response
	 */
	transient SampleResult RESULT = null;
	/**
	 * The XML document
	 */
	protected Document XMLMSG = null;
	/**
	 * size of File[] array
	 */
	private int FILE_COUNT = -1;
	/**
	 * List of files that have .xml extension
	 */
	private File[] FILE_LIST = null;
	/**
	 * Random class for generating random
	 * numbers.
	 */
	private Random RANDOM = new Random();
	/**
	 * We make DocumentBuilder static. I'm not
	 * that this is thread safe. Should
	 * investigate this further to make sure
	 * it's ok. Making it non-static could
	 * mean a performance hit to get a new
	 * DocumentBuilder for each request.
	 * If it's not safe to use static here,
	 * then we should consider using Apache
	 * commons pool to create a pool of
	 * document builders or make sure
	 * XMLParserUtils creates builders
	 * efficiently.
	 */
	protected static DocumentBuilder XDB = null;
	 	
	/**
	 * Set the path where XML messages are
	 * stored for random selection.
	 */
	public void setXmlPathLoc(String path){
		setProperty(XML_PATH_LOC,path);
	}
	
	/**
	 * Get the path where XML messages are
	 * stored. this is the directory where
	 * JMeter will randomly select a file.
	 */
	public String getXmlPathLoc(){
		return getPropertyAsString(XML_PATH_LOC);
	}
	
	/**
	 * it's kinda obvious, but we state it anyways.
	 * Set the xml file with a string path.
	 * @param String filename
	 */
	public void setXmlFile(String filename)
	{
		setProperty(XML_DATA_FILE,filename);
	}
    
    /**
     * Get the file location of the xml file.
     * @return String file path.
     */
	public String getXmlFile()
	{
		return getPropertyAsString(XML_DATA_FILE);
	}

	/**
	 * Method uses jorphan TextFile class to load
	 * the contents of the file. I wonder if we should
	 * cache the DOM Document to save on parsing
	 * the message. Parsing XML is CPU intensive, so
	 * it could restrict the number of threads a
	 * test plan can run effectively. To cache the
	 * documents, it may be good to have an external
	 * class to provide caching that is efficient.
	 * We could just use a HashMap, but for large
	 * tests, it will be slow. Ideally, the cache
	 * would be indexed, so that large tests will
	 * run efficiently.
	 * @return String contents of the file
	 */
	private String retrieveRuntimeXmlData()
	{
		String file = getRandomFileName();
		if(file.length() > 0)
		{
			TextFile contents = new TextFile(file);
			if(contents.exists())
			{
				return contents.getText();
			}
		}
		return getXmlData();
	}

	/**
	 * Method is used internally to check if a random
	 * file should be used for the message. Messages
	 * must be valid. This is one way to load test
	 * with different messages. The limitation of this
	 * approach is parsing XML takes CPU resources,
	 * so it could affect JMeter GUI responsiveness.
	 * @return String filename
	 */
	protected String getRandomFileName(){
		if (this.getXmlPathLoc() != null){
			File src = new File(this.getXmlPathLoc());
			if (src.isDirectory() && src.list() != null) {
				FILE_LIST = src.listFiles(new JMeterFileFilter(new String[]{".xml"}));
				this.FILE_COUNT = FILE_LIST.length;
				File one = FILE_LIST[RANDOM.nextInt(FILE_COUNT)];
				// return the absolutePath of the file
				return one.getAbsolutePath();
			} else {
				return getXmlFile();
			}
		} else {
			return getXmlFile();
		}
	}
	
	/**
	 * set the XML data
	 * @param String data
	 */
	public void setXmlData(String data)
	{
		setProperty(XML_DATA,data);
	}

	/**
	 * get the XML data as a string
	 * @return String data
	 */
	public String getXmlData()
	{
		return getPropertyAsString(XML_DATA);
	}

	/**
	 * set the soap action which should be in
	 * the form of an URN
	 * @param String data
	 */
	public void setSoapAction(String data){
		setProperty(SOAP_ACTION,data);
	}

	/**
	 * return the soap action string
	 * @return String soap action
	 */
	public String getSoapAction() {
		return getPropertyAsString(SOAP_ACTION);
	}

	/**
	 * set the memory cache
	 * @param boolean cache
	 */
	public void setMemoryCache(boolean cache){
		setProperty(MEMORY_CACHE,String.valueOf(cache));
	}

	/**
	 * get the memory cache
	 * @return boolean cache
	 */	
	public boolean getMemoryCache(){
		return getPropertyAsBoolean(MEMORY_CACHE);
	}

	/**
	 * Set whether the sampler should read the
	 * response or not.
	 * @param boolean read
	 */
	public void setReadResponse(boolean read){
		setProperty(READ_RESPONSE,String.valueOf(read));
	}
	
	/**
	 * Return whether or not to read the response
	 * @return boolean
	 */
	public boolean getReadResponse(){
		return this.getPropertyAsBoolean(READ_RESPONSE);
	}
	
	/**
	 * This method uses Apache soap util to create
	 * the proper DOM elements.
	 * @return Element
	 */	
	public org.w3c.dom.Element createDocument(){
		if (getPropertyAsBoolean(MEMORY_CACHE)){
			String next = this.getRandomFileName();
			if (DOMPool.getDocument(next) != null){
				return ((Document)DOMPool.getDocument(next)).getDocumentElement();
			} else {
				return openDocument(next).getDocumentElement();
			}
		} else {
			return openDocument(null).getDocumentElement();
		}
	}

	/**
	 * Open the file and create a Document.
	 * @return Document
	 */
	protected Document openDocument(String key){
		String xmlData = retrieveRuntimeXmlData();
		if (xmlData != null && xmlData.length() > 0){
			Document doc = null;
			try {
				if (XDB == null){
					XDB = XMLParserUtils.getXMLDocBuilder();
				}
				doc = XDB.parse(new InputSource(new StringReader(xmlData)));
				// cache the document
				if (this.getPropertyAsBoolean(MEMORY_CACHE)){
					DOMPool.putDocument(key,doc);
				}
			} catch (Exception ex){
				// ex.printStackTrace();
			}
			return doc;
		} else {
			return null;
		}
	}
	
	/**
	 * sample(Entry e) simply calls sample().
	 * @param Entry e
	 */
	public SampleResult sample(Entry e) {
		return sample();
	}

	/**
	 * sample() does the following: create a
	 * new SampleResult, call sampleWithApache,
	 * and return the result.
	 * @return SampleResult
	 */
	public SampleResult sample() {
		RESULT = new SampleResult();
		sampleWithApache();
		return RESULT;
	}

	/**
	 * Sample the URL using Apache SOAP driver. Implementation
	 * note for myself and those that are curious. Current
	 * logic marks the end after the response has been read.
	 * If read response is set to false, the buffered reader
	 * will read, but do nothing with it. Essentially, the
	 * stream from the server goes into the ether.
	 */
	public void sampleWithApache() {
		try {
			Envelope msgEnv = Envelope.unmarshall(createDocument());

			// send the message
			Message msg = new Message ();
			long start = System.currentTimeMillis();
			msg.send(this.getUrl(), this.getSoapAction(), msgEnv);

			SOAPTransport st = msg.getSOAPTransport ();
			BufferedReader br = st.receive();
			RESULT.setDataType(SampleResult.TEXT);
			if (this.getPropertyAsBoolean(READ_RESPONSE)){
				StringBuffer buf = new StringBuffer();
				String line;
				while((line = br.readLine()) != null){
					buf.append(line);
				}
				RESULT.setTime(System.currentTimeMillis() - start);
				// set the response
				RESULT.setResponseData(buf.toString().getBytes());
			} else {
				// by not reading the response
				// for real, it improves the
				// performance on slow clients
				br.read();
				RESULT.setTime(System.currentTimeMillis() - start);
				RESULT.setResponseData(JMeterUtils.getResString("read_response_message").getBytes());
			}
			RESULT.setTimeStamp(System.currentTimeMillis());
			RESULT.setSuccessful(true);
			RESULT.setSamplerData(getUrl().getProtocol() + "://" + getUrl().getHost() + "/" + getUrl().getFile());
			RESULT.setDataEncoding(st.getResponseSOAPContext().getContentType());
			// setting this is just a formality, since
			// soap will return a descriptive error
			// message, soap errors within the response
			// are preferred.
			RESULT.setResponseCode("200");
			br.close();
			msg = null;
			st = null;
			// reponse code doesn't really apply, since
			// the soap driver doesn't provide a
			// response code
		} catch (Exception exception){
			// exception.printStackTrace();
			RESULT.setSuccessful(false);
		}
	}

	/**
	 * We override this to prevent the wrong encoding
	 * and provide no implementation. We want to
	 * reuse the other parts of HTTPSampler, but not
	 * the connection. The connection is handled by
	 * the Apache SOAP driver.
	 */
	public void addEncodedArgument(String name, String value, String metaData)	{
	}

	/**
	 * We override this to prevent the wrong encoding
	 * and provide no implementation. We want to
	 * reuse the other parts of HTTPSampler, but not
	 * the connection. The connection is handled by
	 * the Apache SOAP driver.
	 */
	protected HttpURLConnection setupConnection(URL u, String method)
		throws IOException
	{
		return null;
	}
	
	/**
	 * We override this to prevent the wrong encoding
	 * and provide no implementation. We want to
	 * reuse the other parts of HTTPSampler, but not
	 * the connection. The connection is handled by
	 * the Apache SOAP driver.
	 */
	protected long connect() throws IOException
	{
		return -1;
	}

}

