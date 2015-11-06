angular.module('todoApp', [])
  .controller('todoController', function($scope) {
    $scope.tasks = []
    $scope.model = {
      nextId: 0,
      tasks: {}
    }
    $scope.add = function() {
      add($scope.model, $scope.description)
      $scope.description = "";
    }
    $scope.getTotalTasks = function() {
      return totalTasksRemaining($scope.model);
    }
    $scope.toggleDone = function(task) {
      task.done = !task.done;
    }
    $scope.removeTask = function(task) {
      delete $scope.model.tasks[task.id];
    }
  });


function totalTasksRemaining(model) {
  var count = 0;
  for (var property in model.tasks) {
    if (model.tasks.hasOwnProperty(property)) {
      if (!model.tasks[property].done) {
        count++;
      }
    }
  }
  return count;
}

function add(model, description) {
  var id = model.nextId++;
  model.tasks[id] = {
    id: id,
    description: description,
    done: false
  };
}