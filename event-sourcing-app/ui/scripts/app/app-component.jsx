import React from 'react';
import ReactDOM from 'react-dom';
import axios from 'axios';
import TagManager from './views/tag-manager.jsx';
import MyComponent from './tutorial/text-component.jsx';
import {createStore} from 'redux';
import {provider} from 'react-redux';

class AppComponent {
  init = () => {
    this.initLoginRedirecting();
    this.renderComponent();
    this.connectToSSEEndpoint();
    this.renderComponent();
  };
  initLoginRedirecting = () => {
    axios.interceptors.response.use((response) => {
      return response;
    }, (error) => {
      if (error.response.status === 401) {
        window.location = '/login';
      }
      return Promise.reject(error);
    });
  };

  renderComponent = () => {
    const reactDiv = document.getElementById('reactDiv');
    if (!!reactDiv) {
      ReactDOM.render(<Provider store={this.store}>
        <TagManager />
      </Provider>, reactDiv);
    }
  }

  initAppState = () => {
    const initialState = {
      tags: []
    };
    const reducer = (state = initialState, action) => {
      const updatedState = {...state};
      const actionType = action.type;
      if (actionType == 'tags_updated') {
        updatedState['tags'] = action.data;
      }
      return updatedState;
    };
    this.store = createStore(reducer);
  };

  connectToSSEEndpoint = () => {
    this.es = new EventSource("/api/sse");
    this.es.addEventListener("message", this.onServerSideEvent);
  };

  onServerSideEvent = (event) => {
    if (event.type == 'message') {
      this.updateReceived(JSON.parse(event.data));
    }
  };

  updateReceived = (data) => {
    if (data['updateType'] == 'tags') {
      this.store.dispatch({
        type: 'tags_updated',
        data: data['updateData']
      });
    }
  };

}

export default AppComponent;
