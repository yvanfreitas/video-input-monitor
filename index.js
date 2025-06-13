import {AppRegistry} from 'react-native';
import App from './App'; // We will create App.js next
import {name as appName} from './app.json'; // We will create app.json next

AppRegistry.registerComponent(appName, () => App);
