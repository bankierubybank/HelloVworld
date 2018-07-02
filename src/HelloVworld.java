import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.HandlerResolver;

import com.vmware.sso.client.soaphandlers.HeaderHandlerResolver;
import com.vmware.sso.client.soaphandlers.TimeStampHandler;
import com.vmware.sso.client.soaphandlers.WsSecuritySignatureAssertionHandler;
import com.vmware.vim25.InvalidLocaleFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import com.vmware.vsphere.soaphandlers.HeaderCookieExtractionHandler;

public class HelloVworld {
	
	static {
	    disableSslVerification();
	}

	private static void disableSslVerification() {
	    try
	    {
	        // Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	            public void checkClientTrusted(X509Certificate[] certs, String authType) {
	            }
	            public void checkServerTrusted(X509Certificate[] certs, String authType) {
	            }
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
					// TODO Auto-generated method stub
					
				}
				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
					// TODO Auto-generated method stub
					
				}
	        }
	        };

	        // Install the all-trusting trust manager
	        SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	        // Create all-trusting host name verifier
	        HostnameVerifier allHostsValid = new HostnameVerifier() {
	            public boolean verify(String hostname, SSLSession session) {
	                return true;
	            }
	        };

	        // Install the all-trusting host verifier
	        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    } catch (KeyManagementException e) {
	        e.printStackTrace();
	    }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url = "https://192.168.126.128/sdk/vimService";
		String username = "root";
		String password = "test@VMware1";

		VimService vimService = new VimService();
		VimPortType vimPort = vimService.getVimPort();
		
		Element token;
		String vcServiceUrl;
		HandlerResolver defaultHandler = vimService.getHandlerResolver();
		HeaderHandlerResolver handlerResolver = new HeaderHandlerResolver();
		HeaderCookieExtractionHandler cookieExtractor = new HeaderCookieExtractionHandler();
		handlerResolver.addHandler(cookieExtractor);
		handlerResolver.addHandler(new TimeStampHandler());
		handlerResolver.addHandler(new SamlTokenHandler(token));
		handlerResolver.addHandler(new WsSecuritySignatureAssertionHandler(
		 userCert.getPrivateKey(),
		 userCert.getUserCert(),
		 Utils.getNodeProperty(token, "ID")));
		vimService.setHandlerResolver(handlerResolver);
		
		Map<String, Object> ctxt = ((BindingProvider) vimPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
		ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

		ManagedObjectReference serviceInstance = new ManagedObjectReference();
		serviceInstance.setType("ServiceInstance");
		serviceInstance.setValue("ServiceInstance");

		try {
			ServiceContent serviceContent = vimPort.retrieveServiceContent(serviceInstance);
			vimPort.login(serviceContent.getSessionManager(), username, password, null);

			XMLGregorianCalendar ct = vimPort.currentTime(serviceInstance);
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			System.out.println("Server Time: " + sdf.format(ct.toGregorianCalendar().getTime()));
			System.out.println("Full name: " + serviceContent.getAbout().getFullName());
			System.out.println("API type: " + serviceContent.getAbout().getApiType());
			System.out.println("API version: " + serviceContent.getAbout().getApiVersion());
			System.out.println("OS: " + serviceContent.getAbout().getOsType());
			
			
		} catch (RuntimeFaultFaultMsg e) {
			e.printStackTrace();
		} catch (InvalidLocaleFaultMsg e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidLoginFaultMsg e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
