library extensionapi;
import 'package:flutter/material.dart';


class MCExtensionAPI {
  String id;
  String title;
  String description;
  int versionCode;
  String versionName;
  List<String> permissionList;
  List<UserInterface> userInterfaces;
  MCExtensionAPI(this.id, {this.title, this.description, this.versionCode, this.versionName, this.permissionList, this.userInterfaces});
//Bitmap icon;
}
class UserInterface{
  final String id;
  final String title;
  final String description;
  final Widget widget;
  final Map<String, dynamic> params;
  UserInterface(this.id,{this.title, this.description, this.widget, this.params});
}

class Configuration{
  Function getConfigurationState;
  Function setConfiguration;
}
class Action{
  String id;
  String title;
  String description;
  Function mcAction;
}