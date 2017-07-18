///
/// Copyright 2017 DataThings - All rights reserved.
///

import {WSClient} from "greycat-websocket";
import {Graph, Node} from "greycat";

export class SecWSClient extends WSClient {

  private _host: string;
  private _port: string;
  private _useSsl: boolean;

  constructor(host: string, port: string, useSsl: boolean, authenticationKey: string) {
    super((useSsl?"wss://":"ws://")+host + ":" + port + "/ws?gc-auth-key=" + authenticationKey.split("#")[0]);
    this._host = host;
    this._port = port;
    this._useSsl = useSsl;

  }

  public static authenticate(host: string, port: string, useSsl: boolean, credentials: Map<string, string>, cb: (authenticated: boolean, authenticationKey: string) => any) {
    let fd = new FormData();

    credentials.forEach((value, key) => fd.append(key, value));

    let xhr: XMLHttpRequest = new XMLHttpRequest();
    xhr.open('POST', (useSsl?"https://":"http://") + host + ":" + port + "/auth");
    xhr.setRequestHeader('Access-Control-Allow-Origin', '*');
    xhr.onerror = (event: ErrorEvent) => {
      if (cb) {
        cb(false, null);
      }
    };
    xhr.onreadystatechange = (event: ProgressEvent) => {
      let xhr = event.target as XMLHttpRequest;
      if (xhr.readyState === 4) {
        if (xhr.status == 200) {
          if (cb) {
            cb(true, xhr.responseText);
          }
        } else {
          if (cb) {
            cb(false, null);
          }
        }
      }
    };
    xhr.send(fd);
  }

  public static renewPassword(host: string, port: string, useSsl: boolean, uuid: string, pass: string, callback: (success: boolean, reason: string) => any) {
    let fd = new FormData();
    fd.append("pass", pass);
    fd.append("uuid", uuid);

    let xhr: XMLHttpRequest = new XMLHttpRequest();
    xhr.open('POST', (useSsl?"https://":"http://") + host + ":" + port + "/renewpasswd");
    xhr.setRequestHeader('Access-Control-Allow-Origin', '*');
    xhr.onreadystatechange = ((event: ProgressEvent) => {
      let localXhr = event.target as XMLHttpRequest;
      if (localXhr.readyState == 4) {
        let result = xhr.responseText.split(":");
        if (xhr.status == 200) {
          callback(true, result[1]);
        } else {
          callback(false, result[1]);
        }
      }
    });
    xhr.send(fd);
  }

  public static getAuthenticatedUser(graph: Graph, authenticationKey: string, cb: (user: Node) => any) {
    if (graph) {
      let userId = authenticationKey;
      if(authenticationKey.indexOf("#") != -1) {
        userId = authenticationKey.split("#")[1];
      }
      graph.lookup(0, (new Date()).getTime(), Number(userId), cb);
    } else {
      cb(null);
    }
  }

}
