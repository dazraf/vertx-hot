angular.module('taskController', [])

// inject the Tasks service factory into our controller
.controller('mainController', function($scope, $http, Tasks) {
  $scope.formData = {};

  var hostAndPort = window.location.href.split('/')[2];
  var ws = new WebSocket("ws://" + hostAndPort + "/api/notifications");

  ws.onopen = function() {
  }
  ws.onmessage = function(message) {
    listener(JSON.parse(message.data));
  }
  ws.onclose = function() {
  }

  // GET =====================================================================
  // when landing on the page, get all task and show them
  // use the service to get all the tasks
  Tasks.get()
  .success(function(tasks) {
    var items = {}
    for (var idx in tasks) {
      var task = tasks[idx];
      items[task._id] = {
        showControls: false,
        data: task
      }
    }
    $scope.items = items;
  });

  // CREATE ==================================================================
  // when submitting the add form, send the text to the node API
  $scope.createTask = function() {

    // validate the formData to make sure that something is there
    // if form is empty, nothing will happen
    if ($scope.formData.text != undefined) {

      // call the create function from our service (returns a promise object)
      Tasks.create({
        description: $scope.formData.text,
        done: false
      })
      // if successful creation, call our get function to get all the new tasks
      .success(function(data) {
        $scope.formData = {}; // clear the form so our user is ready to enter another
      });
    }
  };

  // DELETE ==================================================================
  // delete a task after checking it
  $scope.deleteTask = function(item) {
    Tasks.delete(item.data._id)
    // if successful creation, call our get function to get all the new tasks
    .success(function(data) {
      if (data.status !== "done") {
        console.log("Error");
      }
    });
  };

  $scope.getTotalTasks = function() {
    var count = 0;
    for (var property in $scope.items) {
      if ($scope.items.hasOwnProperty(property)) {
        if (!$scope.items[property].data.done) {
          count++;
        }
      }
    }
    return count;
  }

  $scope.toggleTask = function(item) {
    item.data.done = !item.data.done;
    Tasks.update(item.data);
  }

  $scope.hover = function(item) {
    item.showControls = !item.showControls;
  }

  function listener(data) {
    $scope.$apply(function() {
      if (data.op === "Delete") {
        internalDelete(data.value.id);
      } else if (data.op == "Create") {
        internalUpsert(data.value);
      } else if (data.op = "Update") {
        internalUpsert(data.value);
      }
    })
  }

  function internalDelete(id) {
    delete $scope.items[id];
  }

  function internalUpsert(task) {
    if ($scope.items.hasOwnProperty(task._id)) {
      $scope.items[task._id].data = task;
    } else {
      var item = {
        showControls: false,
        data: task
      };
      $scope.items[task._id] = item; // add the new task
    }
  }
});
