package se.inera.axel.webconsole;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import se.inera.axel.riv.authentication.LoginService;

import javax.inject.Inject;
import javax.inject.Named;

public class BasicAuthenticationSession extends AuthenticatedWebSession {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BasicAuthenticationSession.class);
	private static final long serialVersionUID = 4391047631182868491L;
	private static final Properties props = new Properties();
	private static String [] whitelist;
	private String username;


	public BasicAuthenticationSession(Request request) {
		super(request);
		Injector.get().inject(this);
	}

	@Inject
	@Named("loginService")
	@SpringBean(name = "loginService")
	private LoginService loginService;


	@Override
	public boolean authenticate(String username, String password) {
		if(username == null || password == null)
			return false;

		if(props.isEmpty())
			try {
				loadProperties();
			} catch (Exception e) {
				log.error("Properties can not be loaded. Exception is {}", e.getMessage());
				return false;
			} 

		if(!isWhiteListed())
			return false;

		return loginService.authenticate(username, password);
	}

	
	@Override
	public Roles getRoles() {
		return null;
	}

	/**
	 * Check if remote ip-number matches one item in a list of (partial) ip-numbers.
	 * 
	 * @return
	 */
	private boolean isWhiteListed() {
		String remoteAddress = ((WebClientInfo)Session.get().getClientInfo()).getProperties().getRemoteAddress();
		
		if(whitelist==null || whitelist.length==0)
			return true;
		
		for( String s : whitelist) {
			if(remoteAddress.startsWith(s))
				return true;
		}
		log.error("User with ip-number {} is not on the whitelist", remoteAddress);
		return false;
	}
	
	private void loadProperties() throws FileNotFoundException, IOException {
		
		String home = System.getProperty("axel.home").replace("file:", "");
	    Path path = FileSystems.getDefault().getPath(home, "etc", "security.properties");
		Files.getFileStore(path);

		try(BufferedReader input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			props.load(input);
		};
		
		String wlist = props.getProperty("whitelist");
		if(wlist != null)
			whitelist = wlist.split(",");
	}
}