import type { EmitterSubscription, Task, TaskProvider } from 'react-native';
import { AppRegistry, Platform } from 'react-native';
import { CarPlay } from './CarPlay';

// this headless task is required on Android (Auto) to make sure timers are working fine when screen is off

const headlessTask: TaskProvider = (): Task => _ =>
  new Promise((resolve, reject) => {
    let subscription: EmitterSubscription | null = null;

    try {
      subscription = CarPlay.emitter.addListener('didDisconnect', () => {
        try {
          subscription?.remove();
          resolve();
        } catch (error) {
          console.error('Error in CarPlayHeadlessJsTask didDisconnect listener:', error);
          reject(error);
        }
      });
    } catch (error) {
      console.error('Error in headless task:', error);
      subscription?.remove();
      reject(error);
    }
  });

export default function registerHeadlessTask() {
  if (Platform.OS !== 'android') {
    return;
  }
  AppRegistry.registerHeadlessTask('CarPlayHeadlessJsTask', headlessTask);
}
