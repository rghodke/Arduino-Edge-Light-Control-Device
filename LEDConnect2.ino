/*
 IoT Edge Server for LED RGB Lights Appliance
 
 Requires the Arduino Wiznet Ethernet shield.
 Ethernet shield uses pins 10, 11, 12, 13

 Using Static IP Address and standard HTTP port (80)
 PWM pins for RGB LEDs - 3(red), 5(green) 9(blue)

 Created 26 Sep 2014 by Ranjit Ghodke, serving web pages with slider controls
 Updated 10 Oct 2016 by Ranjit Ghodke, to match Android app using GET request
*/

#include <SPI.h>
#include <Ethernet.h>

// Choose a static IP address on your LAN subnet
#define MY_IP     192,168,0,99
// Select the port for the Ethernet server
#define MY_PORT   80

#define MY_IP_STR "192.168.0.99"
#define PATH_STR  ""

#define RED    "RED"
#define GREEN  "GREEN"
#define BLUE   "BLUE"
#define BRIGHT "BRIGHT"

// pins associated with LED colors
#define PIN_RED   3
#define PIN_GREEN 5
#define PIN_BLUE  9

// max values for each LED color to create white light
#define MAX_RED   255
#define MAX_GREEN 255
#define MAX_BLUE  255

// Enter the MAC address provided with the Ethernet Shield
byte mac[] = {0xDE,0xAD,0xBE,0xEF,0xFE,0xED};
// Set a static IP address on the LAN subnet
IPAddress ip(MY_IP);
// Using the default port 80 for the Ethernet server
EthernetServer server(MY_PORT);
EthernetClient client;
String strRedValue, strBlueValue, strGreenValue, strBrightValue;


void setup() {

  // Initialize serial port
  Serial.begin(9600);
  //while (!Serial) {;} // wait for serial port to connect.

  // Start the Ethernet server
  Ethernet.begin(mac, ip);
  server.begin();

  // Send startup response to console to serial port
  Serial.print("Arduino Server IP Address: ");
  Serial.println(Ethernet.localIP());
  Serial.println("------------------");

  // Set defaults
  strRedValue = "50";
  strBlueValue = "50"; 
  strGreenValue = "50"; 
  strBrightValue="50";

  // Set initial color values to defaults
  setColors();

}


void loop() {

  // listen for incoming clients
  client = server.available();

  if (client) {

    // Initialize loop vars
    String strHttpLine = ""; 
    String strHttpRequest = "";
    boolean currentLineIsBlank = true;

    while (client.connected()) {

      if (client.available()) {
        char c = client.read(); // read one character from the input buffer

        // when end of line is received, process the line
        if (c == '\n') {

          if (currentLineIsBlank) {
            // a blank line marks the end of the HTTP request, 
            // so launch the parser to process the request
            ParseHTTPRequest(strHttpRequest);
            break; // exiting loop, will call close client connection (below)
          }
          else {
            // this is a non blank line, 
            // save only if GET request (POST is not implemented)
            if (strHttpLine.startsWith("GET ")) {
              strHttpRequest = strHttpLine;
            }
            else {
              // skip all other lines (probably headers)
            }
          }

          // clear line buffer for next line
          currentLineIsBlank = true;
          strHttpLine = "";

        }
        else if (c != '\r') {
          // here, c is neither \n nor \r, append c to current line
          strHttpLine.concat(c);
          currentLineIsBlank = false;
        }

      } // end if client.available

    } // end while loop

    // give the web browser time to receive the data
    delay(1); // 1 millisecond

    // close the connection:
    client.stop();
    Serial.println("------------------");

    strHttpRequest = "";
    strHttpLine="";

  } // end if client

}


//
// Typical HTTP request string is: 
//   "GET /path/file.html?querystring#fragment HTTP/1.0"
//
void ParseHTTPRequest(String strHttpRequest) {
  String strRequest, strPath, strQuery, str4, strProtocol;
  int n; 

  // -------------- extract the request ----------------
  n = strHttpRequest.indexOf(" ");
  if (n >= 0) {
    strRequest = strHttpRequest.substring(0,n);       // gets GET
    strHttpRequest = strHttpRequest.substring(n+1);
  }
  else {
    strRequest = strHttpRequest;
    strHttpRequest = "";
  }

  // remove HTTP/1.0 from string
  n = strHttpRequest.indexOf(" ");
  if (n >= 0) {
    strProtocol = strHttpRequest.substring(n+1);    // gets HTTP/1.0
    strHttpRequest = strHttpRequest.substring(0,n); // gets /path/file.html?querystring#fragment
  }
  else {
    strProtocol = "";
  }
  // ---------------------------------------------------

  // separate the path and querystring
  n = strHttpRequest.indexOf("?");
  if (n >= 0) {
    strPath = strHttpRequest.substring(0,n);        // gets /path/file.html
    strHttpRequest = strHttpRequest.substring(n+1); // gets querystring#fragment
  }
  else {
    strPath = strHttpRequest;
    strHttpRequest = "";
  }

  // trim out everything after # in the querystring
  n = strHttpRequest.indexOf("#");
  if (n >= 0) {
    strQuery = strHttpRequest.substring(0,n);     // gets querystring
    str4 = strHttpRequest.substring(n+1);         // gets fragment
  }
  else {
    strQuery = strHttpRequest;                    // gets querystring
    str4 = "";
  }

  // parse name-value pairs from query
  parseQuery(strQuery);

  if (strQuery == "") {
    SendStandardHTTPHeader();
    sendStatus(strQuery); // send the response
  }
  else {
    // update color values
    setColors();
    // send response to web client
    SendStandardHTTPHeader();
    sendStatus(strQuery); // this is the HTML response
  }
}

//
// process all name value pairs in request
//
void parseQuery(String strQuery) {
  int n, n2;
  String str5, strParName, strParValue;

  while (strQuery != "") {
    // extract one name-value pair
    n = strQuery.indexOf("&");
    if (n >= 0) {
      str5 = strQuery.substring(0,n);
      strQuery = strQuery.substring(n+1);
    }
    else {
      str5 = strQuery;
      strQuery = "";
    }

    // separate the name and the value
    n2 = str5.indexOf("=");
    if (n2 > 0) {
      strParName = str5.substring(0,n2);
      strParValue = str5.substring(n2+1);
    }
    else {
      strParName = str5;
      strParValue = "";
    }

    // save values for color and brightness in global variables
    // these will be used to send the response
    strParName.trim();
    strParValue.trim();
    saveParamValue(strParName, strParValue);
  }

}


void saveParamValue(String strParName, String strParValue) {
  // check limits (0, 100)
  int i = strParValue.toInt();
  if (i < 0) i = 0; 
  if (i > 100) i = 100;

  // save specific named values only, ignore everything else
  strParName.toUpperCase();
  if (strParName == RED)    { strRedValue = String(i); }
  if (strParName == GREEN)  { strGreenValue = String(i); }
  if (strParName == BLUE)   { strBlueValue = String(i); }
  if (strParName == BRIGHT) { strBrightValue = String(i); }
}


void setColors () {
  int r, g, b, br;

  // convert color values to integer
  r  = strRedValue.toInt();
  g  = strGreenValue.toInt();
  b  = strBlueValue.toInt();
  br = strBrightValue.toInt();

  // check limits
  //if (r > 100) r = 100;
  //if (g > 100) g = 100;
  //if (b > 100) b = 100;
  if (br > 100) br = 100;

  // apply brightness factor to color values
  r = r * br / 100;
  g = g * br / 100;
  b = b * br / 100;

  // translate color value (0 to 100) to PWM range (0 to MAX, usually 255)
  r = map(r, 0, 100, 0, MAX_RED);
  g = map(g, 0, 100, 0, MAX_GREEN);
  b = map(b, 0, 100, 0, MAX_BLUE);

  // apply each color's PWM value to its pin
  analogWrite(PIN_RED,   r);
  analogWrite(PIN_GREEN, g);
  analogWrite(PIN_BLUE,  b);

  // optional: wait a millisecond
  delay(1);
}

//////////////////////////////////////// RESPONSE FUNCTIONS ////////////////////////////////////////

void SendStandardHTTPHeader() {
  // send a standard http response header
  client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: text/html");
  client.println("Connection: close");  // the connection will be closed after completion of the response
  client.println();
}


void SendErrorHTTPHeader() {
  // send a standard http response header
  client.println("HTTP/1.1 400 Bad Request");
  client.println("Content-Type: text/html");
  client.println("Connection: close");  // the connection will be closed after completion of the response
  client.println();
}


void sendStatus(String strQuery) {
  String str1;
  if (strQuery == "") {
    // send current status
    str1 = "RED=" + strRedValue + "&BLUE=" + strBlueValue + "&GREEN=" + strGreenValue + "&BRIGHT=" + strBrightValue;
    client.println(str1);
  } else {
    // send current query string as response
    //client.println(strQuery);
    // or just send OK
    client.println("OK");
  }
}
