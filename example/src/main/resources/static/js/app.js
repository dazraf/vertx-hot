
var app = {};

app.model = {
  next: 1,
  tasks: {}
}

app.controller = {
  getTasks: function () {
    return app.model.tasks.keys.map(function(v) { return app.model.tasks[v]});
  },

  createTask: function (description) {
    var task = {
      id: app.model.next++,
      description: description,
      done: false
    };
    app.model.tasks[task.id] = task;
    return task;
  },

  deleteTask: function(id) {
    delete app.model.tasks[id];
  }
}