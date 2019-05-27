library extensionapi;
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class MCExtensionAPI {
  String id;
  Map<String, WidgetBuilder> userInterfaces;
  List<PermissionGroup> permissions;
  Future<int> settingsStatus;
  WidgetBuilder settingUI;
  WidgetBuilder mainUI;
  Map<String, Future<dynamic> Function(Map<String, dynamic> defaultConfig, Map<String, dynamic> config, Map<String, dynamic> params)> actions;

  MCExtensionAPI(this.id, {this.mainUI, this.userInterfaces, this.permissions, this.settingsStatus, this.settingUI, this.actions});
}
