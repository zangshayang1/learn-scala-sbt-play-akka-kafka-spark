import React from 'react';
import axios from 'axios';

class TagManager extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      text: '',
      tags: []
    };
  };

  // this override method defines what to do right after the component is mounted
  // axios makes dealing with HTTP request easy for react
  componentDidMount = () => {
    axios.get("/api/tags").then(this.handleResponse);
  };

  handleResponse = (response) => {
    if (response.status == 200) {
      this.setState({
        text: '',
        tags: response.data
      })
    } else {
      console.error(response.statusText);
    }
  };

  addTag = () => {
    const text = this.state.text;
    const isValid = this.state.tags.findIndex((el) => {
      return el.text === text; }) === -1;

    if (isValid) {
      // this is to send data to backend
      // how about render result to frontend?
      axios.post("/api/createTag", { "text" : text }).then(this.handleResponse);
    }
  };

  deleteTag = (id) => { return () => {
    axios.post("/api/deleteTag", { "id" : id }).then(this.handleResponse); };
  };

  handleInput = (event) => {
    this.setState({
      text: event.target.value
    });
  };

  handleKeyPress = (event) => {
    if (event.key === 'Enter') {
      this.addTag();
    }
  };

  render = () => {
    const tags = this.state.tags;
    return <div className="tag-manager">
      <div className="tag-manager__input-panel">
        <div className="tag-manager__input-panel__input">

          <input type="text" className="form-control"
                 onKeyPress={this.handleKeyPress}
                 placeholder="Enter a new tag and press enter"
                 value={this.state.text}
                 onChange={this.handleInput} />
        </div>
      </div>
      <div className="tag-manager__cloud-panel">
        <div className="tag-manager__cloud-panel__available-tags">
          {tags.map( (tag) => {
            // simple render: return <p>{tag.text}</p>
            // <p></p> helps to put each tag in a newline
            return <p><span className="label label-primary"
                         key={tag.id}> {tag.text}
              <a className="remove-tag-link"
                 onClick={this.deleteTag(tag.id)}>x</a></span></p>
          })}
        </div>
      </div>
    </div>
  }

}

export default TagManager;
