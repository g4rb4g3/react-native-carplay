import type { EmitterSubscription, Task, TaskProvider } from 'react-native';
import { AppRegistry, NativeEventEmitter, NativeModules, Platform } from 'react-native';
import { InternalCarPlay } from './interfaces/InternalCarPlay';

// this headless task is required on Android (Auto) to make sure timers are working fine when screen is off

const { RNCarPlay } = NativeModules as { RNCarPlay: InternalCarPlay };
const emitter = new NativeEventEmitter(RNCarPlay);

const headlessTask: TaskProvider = (): Task => _ =>
  new Promise((resolve, reject) => {
    let subscription: EmitterSubscription | null = null;

    try {
      subscription = emitter.addListener('didDisconnect', () => {
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
