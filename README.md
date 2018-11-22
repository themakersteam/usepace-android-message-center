# Android-Message-Center
###Android messaging library 

### 1. Structure

![Screenshot](screenshot.png)

### 2. Setup
* Add jitpack dependency to root build.gradle
    ```bash
        	allprojects {
        		repositories {
        			...
        			maven { url 'https://jitpack.io' }
        		}
        	}
     ```
 
 * Add the dependency 
    ```bash
	    dependencies {
	        implementation 'com.github.UsePace:android-message-center:{latest-version}'
	}
    ```
  
  * Add The following permission to Manifest.xml file 
    ```bash
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" tools:node="replace"/>
    ```
  
  * In case of Manifest Failure, Add this under Application Tag 
    ```bash
     tools:replace="android:theme"
    ```
  
### 3. Usage

#### 3.0 Design
 * For toolbar title override the following string to strings.xml - strings-ar.xml
 
    ```bash
     <string name="message_center_toolbar_title">Title</string>
    ```
 * For colors styling override to colors.xml 
 
     ```bash
     <color name="message_center_primary">{color}</color>
     <color name="message_center_primary_dark">{color}</color>
     <color name="message_center_primary_accent">{color}</color>
     ```

#### 3.1 connect()

 * First Step for integrating the app is to connect on the start of the application  
 
     ```bash
    MessageCenter.connect(Context context, ConnectionRequest connection, ConnectionInterface connectionInterface)
     ```
 
 * Connection Request Object Has the following items 

    *    String app_id; //The Application ID (provided from back-end)
    *    String user_id; // User id (provided from back-end)
    *    String access_token; //Access Token for Security (provided from back-end)
    *    String client; //Message Center is a Client Base Service, The only Client for now is   `MessageCenter.CLIENT_SENDBIRD`
    *    String fcm_token; //The FCM token for notification
    *    String apn_token; //for ios only
    
 * Connection Request Constructors 
    - public ConnectionRequest()
    - public ConnectionRequest(String client, String fcm_token, String app_id, String user_id, String access_token)
    
 * Sample Code for connecting to Message Center    
   
   ```bash
       MessageCenter.connect(this, connectionRequest, new ConnectionInterface() {
                      @Override
                      public void onMessageCenterConnected() {
                          
                      }
      
                      @Override
                      public void onMessageCenterConnectionError(int error_code, MessageCenterException mce) {
      
                      }
                  });
   ```

#### 3.2 join()
 * Joining the chat by url(id) provided from back-end
 
 * Sample code for joining a conversation
    ```bash
    MessageCenter.join(context: this, chat_id: "sample_chat_id"); 
    ```
 * Executing this code will open the chatting window 

#### 3.3 handleNotification()
 * Handles only the related to MessageCenter Notifications 
 
 * Sample code for Handling MessageCenter Notification 
    ```bash
    MessageCenter.handleNotification(context: context,class: Class next, icon: R.mipmap.notifcation, title: "Message App", remotemessage: remoteMessage); 
    ```
 * if app was opened from notification you will get 2 extra fields with the intent 
    * CHANNEL_URL : a string url of the channel a message sent to 
    * FROM_NOTIFICATION : a boolean field defining if message came from notification

#### 3.4 isConnected()

 * returns true if Message Center is connected 
 
 * Sample code for checking connection
    ```bash
    MessageCenter.isConnected();
    ```
 
#### 3.5 disconnect()

 * Disconnects the chat services and stop receiving notifications for chat, can be used on the destroy of the app if necessary 
 
 * Sample code for disconnecting
    ```bash
    MessageCenter.disconnect(new DisconnectInterface() {
                @Override
                public void onMessageCenterDisconnected() {
                    
                }
            });
    ```