import * as React from 'react';
import { Component } from 'react';
import Constants from './constants';
import NotificationItem from './NotificationItem';
import Notification from './Notification';


export interface NotificationContainerProps {
  position: string;
  notifications: Notification[];
  getStyles ?: any;
}
export interface NotificationContainerState {

}

export class NotificationContainer extends Component<any, any> {

  private _style: any = {};

  refs: {
    [key: string]: (NotificationItem)
  };

  componentWillMount() {
    // Fix position if width is overrided
    this._style = this.props.getStyles.container(this.props.position);

    if (this.props.getStyles.overrideWidth && (this.props.position === Constants.positions.tc || this.props.position === Constants.positions.bc)) {
      this._style.marginLeft = -(this.props.getStyles.overrideWidth / 2);
    }
  }

  render() {
    let self = this;
    let notifications: any;

    if ([Constants.positions.bl, Constants.positions.br, Constants.positions.bc].indexOf(this.props.position) > -1) {
      this.props.notifications.reverse();
    }

    notifications = this.props.notifications.map((notification: Notification) => {
      return (
        <NotificationItem
          ref={'notification-' + notification.uid}
          key={notification.uid}
          notification={notification}
          getStyles={self.props.getStyles}
          onRemove={self.props.onRemove}
          noAnimation={self.props.noAnimation}
          allowHTML={self.props.allowHTML}
          children={self.props.children}
        />
      );
    });

    return (
      <div className={'notifications-' + this.props.position} style={this._style}>
        {notifications}
      </div>
    );
  }

}

export default NotificationContainer;

