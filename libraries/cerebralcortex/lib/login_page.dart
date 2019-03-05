import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'server_service.dart';
import 'common_flushbar.dart';
//import 'package:selflytics_decisions/utils/uidata.dart';
import 'package:flushbar/flushbar.dart';

class LoginPage extends StatefulWidget {

  @override
  _LoginPageState createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final username = TextEditingController();
  final password = TextEditingController();
  Flushbar flush;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        elevation: 4.0,
        backgroundColor: Colors.black,
        actions: <Widget>[
          SizedBox(
            width: 5.0,
          ),
          IconButton(
            onPressed: () {
//              Navigator.pushReplacementNamed(context,UIData.routeMainPage);
            },
            icon: Icon(Icons.close),
          )
        ],
      ),
      body: Container(
        // Add box decoration
        decoration: BoxDecoration(
          // Box decoration takes a gradient
          gradient: LinearGradient(
            // Where the linear gradient begins and ends
            begin: Alignment.topLeft,
            end: Alignment.bottomLeft,
            // Add one stop for each color. Stops should increase from 0 to 1
//        stops: [0.1, 0.5, 0.7, 0.9],
            colors: [
              // Colors are easy thanks to Flutter's Colors class.
              Colors.black,
              Colors.teal.shade900,
            ],
          ),
        ),
        child: Center(child: loginBody(context)),
      ),
    );
  }

  loginBody(BuildContext context) => SingleChildScrollView(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[loginHeader(), loginFields(context)],
        ),
      );

  loginHeader() => Column(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Icon(
            FontAwesomeIcons.heartbeat,
            color: Colors.amber,
            size: 80,
          ),
          SizedBox(
            height: 20,
          ),
          Text(
            "Welcome to Decisions",
            style: TextStyle(fontSize: 16, color: Colors.amber),
          ),
          SizedBox(
            height: 10,
          ),
        ],
      );

  loginFields(BuildContext context) => Container(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Container(
              padding: EdgeInsets.symmetric(vertical: 16.0, horizontal: 30.0),
              child: TextField(
                controller: username,
                maxLines: 1,
                decoration: InputDecoration(
                    labelText: "Username",
                    icon: new Icon(Icons.person_outline)),
              ),
            ),
            Container(
              padding: EdgeInsets.symmetric(vertical: 0.0, horizontal: 30.0),
              child: TextField(
                controller: password,
                maxLines: 1,
                obscureText: true,
                decoration: InputDecoration(
                    labelText: "Password", icon: new Icon(Icons.lock_outline)),
              ),
            ),
            SizedBox(
              height: 30.0,
            ),
            Container(
              padding: EdgeInsets.symmetric(vertical: 0.0, horizontal: 30.0),
              width: double.infinity,
              child: RaisedButton(
                padding: EdgeInsets.all(20.0),
                shape: StadiumBorder(),
                child: Text(
                  "SIGN IN",
                  style: TextStyle(color: Colors.white),
                ),
                color: Colors.teal,
                onPressed: () {
                  tryLogin(context, username.text, password.text);
                  //                  Navigator.pop(context);
//                  Navigator.pushNamed(context, UIData.routeStepSettings);
                },
              ),
            ),
            SizedBox(
              height: 10.0,
            ),
 /*           Text(
              "or",
              style: TextStyle(color: Colors.grey, fontSize: 16),
            ),
            SizedBox(
              height: 10.0,
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              mainAxisSize: MainAxisSize.max,
              children: <Widget>[
                Container(
                  child: RaisedButton(
                    padding:
                        EdgeInsets.only(left: 50, top: 8, right: 50, bottom: 8),
                    shape: StadiumBorder(),
                    child: Column(
                      // Replace with a Row for horizontal icon + text
                      children: <Widget>[
                        Icon(FontAwesomeIcons.google),
                        Text("Google")
                      ],
                    ),
                    color: Colors.red,
                    onPressed: () {},
                  ),
                ),
                Container(
                  child: RaisedButton(
                    padding:
                        EdgeInsets.only(left: 50, top: 8, right: 50, bottom: 8),
                    shape: StadiumBorder(),
                    child: Column(
                      // Replace with a Row for horizontal icon + text
                      children: <Widget>[
                        Icon(FontAwesomeIcons.facebook),
                        Text("Facebook")
                      ],
                    ),
                    color: Color.fromRGBO(139, 157, 195, 100),
                    onPressed: () {},
                  ),
                ),
              ],
            ),*/
            SizedBox(
              height: 80.0,
            ),
          ],
        ),
      );

/*
  void prepareConfig(BuildContext context) {
*/
/*
    showDialog(
      context: context,
      barrierDismissible: false,
      child: new Dialog(
        child: new Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            new CircularProgressIndicator(),
            new Text("Preparing..."),
          ],
        ),
      ),
    );
*//*

    ServerService().downloadConfig().then((onValue) {
//      Navigator.pop(context); //pop dialog
      Navigator.pop(context);
      Navigator.pushNamed(context, UIData.routeMainPage);
    }).catchError((e) {
      CommonFlushBar().showError(context, e);
    });
  }
*/

  void tryLogin(BuildContext context, String username, String password) async {
/*
    showDialog(
      context: context,
      barrierDismissible: false,
      child: new Dialog(
        child: new Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            new CircularProgressIndicator(),
            new Text("Signing in..."),
          ],
        ),
      ),
    );
*/
    if (username.length == 0) {
      if(flush!=null && !flush.isDismissed())
        flush.dismiss(true);
      flush = CommonFlushBar().showError(context, "Invalid Username");
//      CommonFlushBar().showError(context, "Invalid Username");
    } else if (password.length == 0) {
      if(flush!=null && !flush.isDismissed())
        flush.dismiss(true);
      flush = CommonFlushBar().showError(context, "Invalid Password");
    } else {
      if(flush!=null && !flush.isDismissed())
        flush.dismiss(true);
      flush = CommonFlushBar().showProgress(context, "Signing in...");
      ServerService().login(username, password).then((onValue) {
        if(flush!=null && !flush.isDismissed())
          flush.dismiss(true);
          flush = CommonFlushBar().showSuccess(context, "Login successful");
//        Navigator.pushReplacementNamed(context,UIData.routeMainPage);
//          prepareConfig(context);
      }).catchError((onError) {
        if(flush!=null && !flush.isDismissed())
          flush.dismiss(true);
        flush = CommonFlushBar().showError(context, onError);
      });
    }
  }

  @override
  void dispose() {
    // Clean up the controller when the Widget is disposed
    username.dispose();
    password.dispose();
    super.dispose();
  }
}
