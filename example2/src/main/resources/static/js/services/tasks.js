angular.module('taskService', [])
  .factory('Tasks', function ($http) {
    return {
      get: function() {
        return $http.get("/api/task");
      },
      create : function(task) {
        return $http.post("/api/task", task);
      },
      delete : function(id) {
        return $http.delete("/api/task/" + id);
      },
      update: function(task) {
        return $http.put("/api/task/" + task._id, task)
      },
      done: function(id, done) {
        return $http.put("/api/task/" + id, { done: done });
      }
    }
  })
