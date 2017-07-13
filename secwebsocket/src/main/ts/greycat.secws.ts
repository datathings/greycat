///
/// Copyright 2017 DataThings - All rights reserved.
///

import {WSClient} from "greycat-websocket";
import {Graph, Node} from "greycat";

export class SecWSClient extends WSClient {
  constructor(p_url: string, authenticationKey: string) {
    super(p_url + "?gc-auth-key=" + authenticationKey.split("#")[0]);
  }

  public static authenticate(url: string, login: string, pass: string, cb: (authenticated: boolean, authenticationKey: string) => any) {
    let fd = new FormData();
    fd.append("login", login);
    fd.append("pass", pass);

    let xhr: XMLHttpRequest = new XMLHttpRequest();
    xhr.open('POST', url + "/auth");
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

  public static renewPassword(url: string, uuid: string, pass: string, callback: (success: boolean, reason: string) => any) {
    let fd = new FormData();
    fd.append("pass", pass);
    fd.append("uuid", uuid);

    let xhr: XMLHttpRequest = new XMLHttpRequest();
    xhr.open('POST', url + "/renewpasswd");
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
      graph.lookup(0, (new Date()).getTime(), Number(authenticationKey.split("#")[1]), cb);
    } else {
      cb(null);
    }
  }

}


