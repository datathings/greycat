import * as React from 'react';
import { Component } from 'react';
import Constants from './constants';
import Styles, { NotificationStyle } from './styles';
import NotificationContainer from './NotificationContainer';
import Notification from './Notification';

export interface NotificationSystemProps {
  style ?: NotificationStyle; // boolean | any;
  noAnimation ?: boolean;
  allowHTML ?: boolean;
}

export interface NotificationSystemState {
  notifications: Notification[];
}

export class NotificationSystem extends Component<NotificationSystemProps, NotificationSystemState> {

  refs: {
    [key: string]: (NotificationContainer)
  };

  private _isMounted: boolean = false;
  private uid: number = 3400;

  private _getStyles: any = {
    overrideStyle: {},
    overrideWidth: -1,

    setOverrideStyle(style: any) {
      this.overrideStyle = style;
    },

    wrapper() {
      if (!this.overrideStyle) {
        return {};
      }
      return Object.assign({}, Styles.Wrapper, this.overrideStyle.Wrapper);
    },

    container(position: any) {
      let override = this.overrideStyle.Containers || {};
      if (!this.overrideStyle) {
        return {};
      }

      this.overrideWidth = Styles.Containers.DefaultStyle.width;

      if (override.DefaultStyle && override.DefaultStyle.width) {
        this.overrideWidth = override.DefaultStyle.width;
      }

      if (override[position] && override[position].width) {
        this.overrideWidth = override[position].width;
      }

      return Object.assign(
        {}, Styles.Containers.DefaultStyle, Styles.Containers[position], override.DefaultStyle, override[position]);
    },

    elements: {
      notification  : 'NotificationItem',
      title         : 'Title',
      messageWrapper: 'MessageWrapper',
      dismiss       : 'Dismiss',
      action        : 'Action',
      actionWrapper : 'ActionWrapper'
    },

    byElement(element: any) {
      let self = this;
      return function (level: number) {
        let _element = self.elements[element];
        let override = self.overrideStyle[_element] || {};
        if (!self.overrideStyle) {
          return {};
        }
        return Object.assign(
          {}, Styles[_element].DefaultStyle, Styles[_element][level], override.DefaultStyle, override[level]);
      };
    }
  };

  constructor(props: NotificationSystemProps) {
    super(props);
    this.state = {notifications: []};
  }

  componentDidMount() {
    this._getStyles.setOverrideStyle(this.props.style);
    this._isMounted = true;
  }

  componentWillUnmount() {
    this._isMounted = false;
  }

  public addNotification(notification: Notification): Notification | null {
    let _notification = Object.assign(new Notification(), notification);
    let notifications = this.state.notifications;
    let i;

    if (!_notification.level) {
      throw new Error('notification level is required.');
    }

    if (Object.keys(Constants.levels).indexOf(_notification.level) === -1) {
      throw new Error('\'' + _notification.level + '\' is not a valid level.');
    }

    if (isNaN(_notification.autoDismiss)) {
      throw new Error('\'autoDismiss\' must be a number.');
    }

    if (Object.keys(Constants.positions).indexOf(_notification.position) === -1) {
      throw new Error('\'' + _notification.position + '\' is not a valid position.');
    }

    // Some preparations
    _notification.position = _notification.position.toLowerCase();
    _notification.level = _notification.level.toLowerCase();
    _notification.autoDismiss = _notification.autoDismiss;

    _notification.uid = _notification.uid || this.uid;
    _notification.ref = 'notification-' + _notification.uid;
    this.uid += 1;

    // do not add if the notification already exists based on supplied uid
    for (i = 0; i < notifications.length; i++) {
      if (notifications[i].uid === _notification.uid) {
        return null;
      }
    }

    notifications.push(_notification);

    if (typeof _notification.onAdd === 'function') {
      notification.onAdd(_notification);
    }

    this.setState({
      notifications: notifications
    });

    return _notification;
  }

  public removeNotification(notification: Notification) {
    let self = this;
    Object.keys(this.refs).forEach((container) => {
      if (container.indexOf('container') > -1) {
        Object.keys(self.refs[container].refs).forEach((_notification) => {
          let uid = notification.uid ? notification.uid : notification;
          if (_notification === 'notification-' + uid) {
            self.refs[container].refs[_notification]._hideNotification();
          }
        });
      }
    });
  }

  public clearNotifications() {
    let self = this;
    Object.keys(this.refs).forEach((container) => {
      if (container.indexOf('container') > -1) {
        Object.keys(self.refs[container].refs).forEach((_notification) => {
          self.refs[container].refs[_notification]._hideNotification();
        });
      }
    });
  }

  render() {
    let self = this;
    let containers = null;
    let notifications = this.state.notifications;

    if (notifications.length) {
      containers = Object.keys(Constants.positions).map((position) => {
        let _notifications = notifications.filter((notification) => {
          return position === notification.position;
        });

        if (!_notifications.length) {
          return null;
        }

        return (
          <NotificationContainer
            ref={'container-' + position}
            key={position}
            position={position}
            notifications={_notifications}
            getStyles={self._getStyles}
            onRemove={self._didNotificationRemoved.bind(self)}
            noAnimation={self.props.noAnimation}
            allowHTML={self.props.allowHTML}
          />
        );
      });
    }

    return (
      <div className="notifications-wrapper" style={this._getStyles.wrapper()}>
        {containers}
      </div>

    );
  }

  private _didNotificationRemoved(uid: number) {
    let notification = this.state.notifications.find((value, index, obj) => value.uid === uid);
    let notifications = this.state.notifications.filter((value, index, obj) => value.uid !== uid);

    if (notification && notification.onRemove) {
      notification.onRemove(notification);
    }

    if (this._isMounted) {
      this.setState({notifications: notifications});
    }
  }

}
export default NotificationSystem;
