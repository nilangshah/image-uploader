var app = angular.module('upload', []);
app.directive('fileUpload', function($http, $window) {
	return {
		templateUrl : '../uploadFileView.html',
		link : function($scope, element) {
			$scope.fileName = 'Choose a file...';
			element.bind('change', function() {
				$scope.$apply(function() {
					$scope.formData = new FormData();
					$scope.fileName = document
							.getElementById('uploadFileInput').files[0].name;
					$scope.formData.append('file', document
							.getElementById('uploadFileInput').files[0]);
				});
			});
		}
	}
});

app
		.controller(
				'uploadControl',
				function($scope, $http) {
					$scope.uploadFile = function() {
						if ($scope.fileName === "Choose a file...") {
							bootbox.alert("Choose a file to upload");
						} else {
							var dialog = bootbox
									.alert({
										message : '<p><i class="fa fa-spin fa-spinner"></i> Uploading...</p>'
									});

							console.log($scope.formData);
							var fileRegex = /.(jpg|jpeg|png|gif)$/i;
							if (fileRegex.test($scope.fileName)) {
								// Add code to submit the formData
								var data = $scope.formData;
								var config = {
									headers : {
										'Content-Type' : undefined
									}
								}
								$http
										.post('/images', data, config)
										.success(
												function(data, status, headers,
														config) {
													console
															.log("Image uploaded successfully");
													dialog
															.find(
																	'.bootbox-body')
															.html(
																	'Image uploaded successfully');
													$scope.fileName = 'Choose a file...';
												})
										.error(
												function(data, status, header,
														config) {
													console
															.log("Image uploaded failed");
													dialog
															.find(
																	'.bootbox-body')
															.html(
																	'Image uploaded failed');
												});
							} else {
								dialog
										.find('.bootbox-body')
										.html(
												"File extension is invalid. Only jpg|png|gif|bmp|jpeg supported");
							}
						}
					}
				});

app.controller('displaycontrol', function($scope, $http) {
	$scope.records = [];
	var config = {
		headers : {
			'Content-Type' : 'application/json; charset=UTF-8'
		}
	};

	$scope.getImages = function(token) {
		var url = "/images";
		if (token) {
			url = url + "?token=" + token;
		}
		console.log(url);
		$http.get(url, config).success(function(data, status, headers, config) {
			$scope.records = data.imageKeys;
			$scope.token = data.token;
			console.log($scope.records);
			console.log(data.token);
		}).error(function(data, status, header, config) {
			console.log(data);
		});
	}

	$scope.getImages();

	$scope.start = function() {
		$scope.getImages();
	}

	$scope.nextImages = function(token) {
		$scope.getImages(token);
	}

});