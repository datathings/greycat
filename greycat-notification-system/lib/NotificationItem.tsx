import * as React from 'react';
import {Component} from 'react';
import * as ReactDOM from 'react-dom';
import Constants from './constants';
import Helpers from './helpers';
import Notification from './Notification';

/* From Modernizr */
function whichTransitionEvent() {
  let el = document.createElement('fakeelement');
  let transition: string;
  let transitions = {
    'transition': 'transitionend',
    'OTransition': 'oTransitionEnd',
    'MozTransition': 'transitionend',
    'WebkitTransition': 'webkitTransitionEnd'
  };

  Object.keys(transitions).forEach(function (transitionKey: string) {
    if ((el as HTMLElement).style[transitionKey] !== undefined) {
      transition = transitions[transitionKey];
    }
  });
  return transition;
}

export interface NotificationItemProps {
  notification: Notification;
  getStyles: any;
  onRemove: (notificationId: number) => void;
  allowHTML: boolean;
  noAnimation: boolean;
  children: string | Element;
}

export interface NotificationItemState {
  visible: boolean;
  removed: boolean;
}

export class NotificationItem extends Component<NotificationItemProps, NotificationItemState> {

  public static defaultProps: Partial<NotificationItemProps> = {
    noAnimation: false,
    onRemove: () => {
    },
    allowHTML: false
  };

  private _styles: any = {};
  private _notificationTimer: Helpers.Timer;
  private _height: number = 0;
  private _noAnimation: boolean = false;
  private _isMounted: boolean = false;
  private _removeCount: number = 0;

  constructor(props: NotificationItemProps) {
    super(props);
    this.state = {
      visible: false,
      removed: false
    };
  }

  componentWillMount() {
    let getStyles = this.props.getStyles;
    let level = this.props.notification.level;

    this._noAnimation = this.props.noAnimation;

    this._styles = {
      notification: getStyles.byElement('notification')(level),
      title: getStyles.byElement('title')(level),
      dismiss: getStyles.byElement('dismiss')(level),
      messageWrapper: getStyles.byElement('messageWrapper')(level),
      actionWrapper: getStyles.byElement('actionWrapper')(level),
      action: getStyles.byElement('action')(level)
    };

    if (!this.props.notification.dismissible) {
      this._styles.notification.cursor = 'default';
    }
  }

  componentDidMount() {
    let transitionEvent = whichTransitionEvent();
    let notification = this.props.notification;
    let element = ReactDOM.findDOMNode(this) as HTMLElement;

    this._height = element.offsetHeight;

    this._isMounted = true;

    // Watch for transition end
    if (!this._noAnimation) {
      if (transitionEvent) {
        element.addEventListener(transitionEvent, this._onTransitionEnd.bind(this));
      } else {
        this._noAnimation = true;
      }
    }

    if (notification.autoDismiss) {
      this._notificationTimer = new Helpers.Timer(
        this._hideNotification.bind(this),
        notification.autoDismiss * 1000);
    }

    this._showNotification();
  }

  private _getCssPropertyByPosition(): { property: string, value: number } {
    let position = this.props.notification.position;
    let css: { property: string, value: number } = null;

    switch (position) {
      case Constants.positions.tl:
      case Constants.positions.bl:
        css = {
          property: 'left',
          value: -200
        };
        break;

      case Constants.positions.tr:
      case Constants.positions.br:
        css = {
          property: 'right',
          value: -200
        };
        break;

      case Constants.positions.tc:
        css = {
          property: 'top',
          value: -100
        };
        break;

      case Constants.positions.bc:
        css = {
          property: 'bottom',
          value: -100
        };
        break;

      default:
    }

    return css;
  }

  private _defaultAction(event: any) {
    let notification = this.props.notification;
    event.preventDefault();
    this._hideNotification();
    if (typeof notification.action.callback === 'function') {
      notification.action.callback();
    }
  }

  public _hideNotification() {
    if (this._notificationTimer) {
      this._notificationTimer.clear();
    }

    if (this._isMounted) {
      this.setState({
        visible: false,
        removed: true
      });
    }

    if (this._noAnimation) {
      this._removeNotification();
    }
  }

  private _removeNotification() {
    this.props.onRemove(this.props.notification.uid);
  }

  private _dismiss() {
    if (!this.props.notification.dismissible) {
      return;
    }

    this._hideNotification();
  }

  private _showNotification() {
    setTimeout(
      () => {
        if (this._isMounted) {
          this.setState({
            visible: true
          });
        }
      },
      50);
  }

  private _onTransitionEnd() {
    if (this._removeCount > 0) {
      return;
    }
    if (this.state.removed) {
      this._removeCount++;
      this._removeNotification();
    }
  }

  private _handleMouseEnter() {
    let notification = this.props.notification;
    if (notification.autoDismiss) {
      this._notificationTimer.pause();
    }
  }

  private _handleMouseLeave() {
    let notification = this.props.notification;
    if (notification.autoDismiss) {
      this._notificationTimer.resume();
    }
  }

  componentWillUnmount() {
    let element = ReactDOM.findDOMNode(this);
    let transitionEvent = whichTransitionEvent();
    element.removeEventListener(transitionEvent, this._onTransitionEnd.bind(this));
    this._isMounted = false;
  }

  private _allowHTMLFc(value: string) {
    return {__html: value};
  }

  render() {
    let notification = this.props.notification;
    let className = 'notification notification-' + notification.level;
    let notificationStyle = Object.assign({}, this._styles.notification);
    let cssByPos = this._getCssPropertyByPosition();
    let dismiss: JSX.Element = null;
    let actionButton: JSX.Element = null;
    let title: JSX.Element = null;
    let message: JSX.Element = null;

    if (this.state.visible) {
      className += ' notification-visible';
    } else if (this.state.visible === false) {
      className += ' notification-hidden';
    }

    if (!notification.dismissible) {
      className += ' notification-not-dismissible';
    }

    if (this.props.getStyles.overrideStyle) {
      if (!this.state.visible && !this.state.removed) {
        notificationStyle[cssByPos.property] = cssByPos.value;
      }

      if (this.state.visible && !this.state.removed) {
        notificationStyle[cssByPos.property] = 0;
      }

      if (this.state.removed) {
        notificationStyle.overlay = 'hidden';
        notificationStyle.height = 0;
        notificationStyle.marginTop = 0;
        notificationStyle.paddingTop = 0;
        notificationStyle.paddingBottom = 0;
      }
      notificationStyle.opacity =
        (this.state.visible ? this._styles.notification.isVisible.opacity : this._styles.notification.isHidden.opacity);
    }

    if (notification.title) {
      title = <h4 className="notification-title" style={this._styles.title}>{notification.title}</h4>;
    }

    if (notification.message) {
      if (this.props.allowHTML) {
        message = (
          <div
            className="notification-message"
            style={this._styles.messageWrapper}
            dangerouslySetInnerHTML={this._allowHTMLFc(notification.message)} />
        );
      } else {
        message = (
          <div className="notification-message" style={this._styles.messageWrapper}>{notification.message}</div>
        );
      }
    }

    if (notification.dismissible) {
      dismiss = <span className="notification-dismiss" style={this._styles.dismiss}>&times;</span>;
    }

    if (notification.action) {
      actionButton = (
        <div className="notification-action-wrapper" style={this._styles.actionWrapper}>
          <button
            className="notification-action-button"
            onClick={(e) => this._defaultAction(e)}
            style={this._styles.action}>
            {notification.action.label}
          </button>
        </div>
      );
    }

    if (notification.children) {
      actionButton = notification.children;
    }

    return (
      <div className={className} onClick={() => this._dismiss()} onMouseEnter={() => this._handleMouseEnter()} onMouseLeave={() => this._handleMouseLeave()} style={notificationStyle}>
        {title}
        {message}
        {dismiss}
        {actionButton}
      </div>
    );
  }

}
export default NotificationItem;