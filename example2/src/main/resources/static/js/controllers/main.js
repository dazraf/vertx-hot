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
				var results = {}
				for (var idx in tasks) {
					var task = tasks[idx];
					results[task._id] = task;
				}
				$scope.tasks = results;
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
		$scope.deleteTask = function(id) {
			Tasks.delete(id)
				// if successful creation, call our get function to get all the new tasks
				.success(function(data) {
					if (data.status !== "done") {
						console.log("Error");
					}
				});
		};

		$scope.getTotalTasks = function() {
      var count = 0;
      for (var property in $scope.tasks) {
        if ($scope.tasks.hasOwnProperty(property)) {
          if (!$scope.tasks[property].done) {
            count++;
          }
        }
      }
      return count;
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
			delete $scope.tasks[id];
		}

		function internalUpsert(task) {
			$scope.tasks[task._id] = task; // add the new task
		}
	});


