package fr.ippon.tatami.uitest

import fr.ippon.tatami.test.support.LdapTestServer;
import fr.ippon.tatami.uitest.support.TatamiBaseGebSpec;
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import geb.Page
import geb.Browser
import geb.spock.GebSpec

import pages.*
import pages.google.*;

class FollowerSpec extends TatamiBaseGebSpec {
	
	// TODO : change ldap auth to cassandra provisionning ...
	
	static LdapTestServer ldapTestServer
	def setupSpec() {
		// It only works if Tatami server is on the same host as the test ...
		// AND tatami server points to localhost to reach the ldap server !
		// TODO : fix tatami configuration !
		// TODO : put this into maven
		ldapTestServer = new LdapTestServer();
		ldapTestServer.start();
	}
	
	def cleanupSpec() {
		ldapTestServer.stop();
		ldapTestServer = null;
	}
	
	def "login and follow a user"() {
		given:
		// if I don't specify a driver here, it reuse a global one ... how ???
		// TODO reuse the GebConfig ...
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference( "intl.accept_languages", "en" );
		def driver = new FirefoxDriver(profile) 
		def secondBrowser = new Browser(driver: driver);
		sendStatusWithUserB(secondBrowser)
		
		// user A : 
		to LoginPage
		verifyAt()
		
		when:
		loginForm.with {
			j_username = "userA@ippon.fr"
			j_password = "ippon"
		}
		 
		and:
		loginButton.click()
		 
		waitFor { at HomePage }
		
		to(ProfilePage,"userb/") 
		at ProfilePage
		
		if(followActionButton.text() != 'Followed') {
			followActionButton.click();
		}
		
		to HomePage
		
		then:
		statuses.find("todo", text: startsWith("todo"))
				
		cleanup:
		// TODO : initialize and teardown globally ? and reuse this instance ?
		secondBrowser.close();
	}
	
	def sendStatusWithUserB(secondBrowser) {
		def closure = {
			to LoginPage
			verifyAt()
			loginForm.with {
				j_username = "userB@ippon.fr"
				j_password = "ippon"
			}
			loginButton.click()
			waitFor { at HomePage }
			updateStatusContent << "Here is a status from userB"
			updateStatus.click()
		}
		closure.resolveStrategy = Closure.DELEGATE_FIRST
		Browser.drive(secondBrowser,closure)
	}
}