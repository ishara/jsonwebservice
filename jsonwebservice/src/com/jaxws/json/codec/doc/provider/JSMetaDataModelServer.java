package com.jaxws.json.codec.doc.provider;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.jaxws.json.codec.JSONBindingID;
import com.jaxws.json.codec.JSONCodec;
import com.jaxws.json.codec.doc.HttpMetadataProvider;
import com.jaxws.json.codec.doc.JSONHttpMetadataPublisher;
import com.jaxws.json.codec.encode.WSJSONWriter;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.server.BoundEndpoint;
import com.sun.xml.ws.api.server.Module;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

/**
 * @author Sundaramurthi Saminathan
 * @since JSONWebservice codec version 0.4
 * @version 1.0
 * 
 * Default JSON service end point document provider.
 */
public class JSMetaDataModelServer implements HttpMetadataProvider {
	
	private static final String[] queries = new String[]{"client"};
	
	/**
	 * Map holder which keeps end point documents.
	 */
	private final static Map<QName,String>	endPointDocuments	= Collections.synchronizedMap(new HashMap<QName,String>());
	
	/**
	 * Request received codec instance holder
	 */
	private JSONCodec codec;
	
	
	/**
	 * "jsonmodel" query handled.
	 */
	public String[] getHandlingQueries() {
		return queries;
	}

	/**
	 * Handler flag, If query string is jsonmodel , its handled by model server.
	 */
	public boolean canHandle(String queryString) {
		return queryString != null && queryString.equals(queries[0]);
	}
	
	/**
	 * end point codec set holder.
	 */
	public void setJSONCodec(JSONCodec codec) {
		this.codec	= codec;
	}
	
	/**
	 * setter
	 */
	public void setHttpAdapter(HttpAdapter httpAdapter) {
		// TODO Auto-generated method stub

	}

	/**
	 * Meta data model content provider.
	 * @see HttpMetadataProvider.getContentType
	 */
	public String getContentType() {
		return "text/javascript; charset=\"utf-8\"";
	}

	public void process() {
		Map<String,Object> 	metadataModel 	= new LinkedHashMap<String, Object>();
		WSEndpoint<?> 		endPoint 		= this.codec.getEndpoint();
		Map<String,Object>  service 		= new HashMap<String, Object>();
		metadataModel.put(endPoint.getServiceName().getLocalPart(), service );
		JAXBContextImpl 	context 	= (JAXBContextImpl)endPoint.getSEIModel().getJAXBContext();
		
		Module 				modules 		= endPoint.getContainer().getSPI(com.sun.xml.ws.api.server.Module.class);
		for(BoundEndpoint endPointObj : modules.getBoundEndpoints()){
			if(endPointObj.getEndpoint().getBinding().getBindingID() == JSONBindingID.JSON_BINDING){
				Map<String,Object>   portJSONMap 	= new HashMap<String, Object>();
				service.put(endPointObj.getEndpoint().getPortName().getLocalPart(), portJSONMap);
				
				SEIModel 	seiModel 		= endPointObj.getEndpoint().getSEIModel();
				for (WSDLBoundOperation operation : seiModel.getPort().getBinding().getBindingOperations()) {
					
					Map<String,Object>    operationMap = new HashMap<String, Object>();
					portJSONMap.put(operation.getName().getLocalPart(), operationMap );
					
					operationMap.put(operation.getOperation().getName().getLocalPart(), 
							JSONHttpMetadataPublisher.getJSONAsMap(operation.getInParts(), context));
					
					operationMap.put(operation.getOperation().getOutput().getName(),JSONHttpMetadataPublisher.getJSONAsMap(operation.getOutParts(),
							context));
				}
				
				portJSONMap.put("address", endPoint.getPort().getAddress().toString());
			}
		}
		/*clientCode.append(convertStreamToString(getClass().getResourceAsStream("client.js")));
		
		String methodUrl 	= "";
		if(jsonEndpoint.getPort().getAddress().getURL() != null)
			methodUrl = jsonEndpoint.getPort().getAddress().getURL().getPath();
		String mimeType 	= jsonEndpoint.createCodec().getMimeType();
		for(WSDLOperation operation : jsonPort.getOperations()){
			createClientTemplete(operation,methodUrl,mimeType);
			clientCode.append(",");
		}*/
		
		endPointDocuments.put(this.codec.getEndpoint().getServiceName(),
				WSJSONWriter.writeMetadata(metadataModel, this.codec.getCustomSerializer()));
	}
	
	/**
	 * Output responder.
	 */
	public void doResponse(WSHTTPConnection ouStream) throws IOException {
		process();
		String portDocuments =  endPointDocuments.get(this.codec.getEndpoint().getServiceName());
		if(portDocuments != null){
			ouStream.getOutput().write(portDocuments.getBytes());
		}else{
			ouStream.getOutput().write(String.format("Unable to find default document for %s",
					this.codec.getEndpoint().getPortName()).getBytes());
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(HttpMetadataProvider o) {
		if(o.equals(this)){
			return 0;
		}else{
			return Integer.MAX_VALUE;
		}
	}
	

}
