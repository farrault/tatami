package pages

import geb.Page;

class ProfilePage extends TatamiBasePage {
    static url = "tatami/profile/" // "profile/<login>/"
 
    static at = { followAction != null }
 
    static content = {
		// defined in super-class :
//		dropDownMenu
//    	adminLink

		followAction  { $("div#follow-action") }
		followActionButton { followAction.find("span") }
    } 
}