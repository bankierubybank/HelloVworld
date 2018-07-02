/* **********************************************************
 * Copyright 2012-2014 VMware, Inc.  All rights reserved.
 * **********************************************************/
package com.vmware.sso.client.wssecurity;

import java.security.SignatureException;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 * Interface used for signing the SOAP requests
 * 
 * @author Ecosystem Engineering
 */
public interface WsSecuritySignature {

	/**
	 * Signs the SOAP Message
	 * 
	 * @param message
	 * @return
	 * @throws SignatureException
	 * @throws SOAPException
	 */
	public SOAPMessage sign(SOAPMessage message) throws SignatureException,
			SOAPException;
}
