///
/// Copyright 2017 DataThings - All rights reserved.
///

import {WSClient} from "greycat-websocket";

export class SecWSClient extends WSClient {
  constructor(p_url: string, authKey: string) {
    super(p_url + "?gc-auth-key=" + authKey);
  }

}


