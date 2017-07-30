import NotificationAction from './NotificationAction';
/**
 * Created by gnain on 24/05/2017.
 */

export class Notification {
  public uid?: number;
  public title?: string;
  public message?: string;
  public level?: string;
  public position?: string = 'tr';
  public autoDismiss?: number = 5;
  public dismissible?: boolean = true;
  public children?: JSX.Element;
  public ref?: string;

  public onAdd?: (me: Notification) => void;
  public onRemove?: (self: Notification) => void;

  public action?: NotificationAction = null;

}
export default Notification;
