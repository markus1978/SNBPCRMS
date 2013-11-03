function log(event) {
	document.getElementById("logs").innerHTML = '<p style="margin-bottom: 0px;">[' + new Date().toLocaleString() + '] ' + event + '</p>';                
}

function error(event) {
	document.getElementById("logs").innerHTML = '<p style="margin-bottom: 0px; color: FF0000;">[' + new Date().toLocaleString() + '] ' + event + '</p>';
}

function loadNextPage() {
	var nextPage = $(document).find('#next-page')
	if (typeof nextPage != 'undefined') {
		if (nextPage.attr('isLoading') == 'false') {
			nextPage.attr('isLoading', 'true')
			$.ajax({
				url: nextPage.attr('url'),
				success: function(result) {
					log('Loaded next page for query')
					nextPage.attr('isLoading', 'false')
					var newPage = nextPage.parent().append(result)
					nextPage.remove()
					linkifyAll(newPage.get(), "linkify")
				},
			    error: function(xhr, status, error) {
			    	nextPage.attr('isLoading', 'false')
			    	nextPage.removeAttr('id')
			    	nextPage.children(':first').html('<span class="bad">Could not load next page : ' + error + '. Try reloading.</span>')
			    }
			})
		}
	}
}

$(window).scroll(function() { 
	setTimeout(function() {	
		if ($(window).scrollTop() + $(window).height() > $(document).height() * .75) {   
			loadNextPage()
		}
	}, 100)
})

$(document).ready(function($) {
	$('.pre-query-btn').click(function() {
		$(this).parents('.actions').find('.query').val($(this).attr('query'))
	})
	$('#container').on('click', '.simple-ajax-action', function() {			
	    $.ajax({
	        url: $(this).attr('url'),
	        success:function(result, status, xhr) {
	        	log(result);
	        },
	        error: function(errorThrown){
	        	error(errorThrown);
	        }
	    });
	})
	$('#container').on('change', '.ajax-form', function() {		
		formElements = $(this).parents('.ajax-form-holder').find(':input')
		formElements.prop('disabled', true)
		var data = {}
		form = $(this)
		if ($(this).is(':checkbox')) {
			console.log('is checkbox ' + $(this).prop('checked'))
			data[this.name] = $(this).prop('checked')
		} else {
			data[this.name] = this.value
		}
		$.ajax({
	        url: $(this).attr('url'),
	        type : 'POST',
	        contentType : 'text/json',
	        data: JSON.stringify(data),
	        success: function(result, status, xhr) {
	        	var logMessage = form.attr('ajax-log-message')
	        	if (logMessage == undefined) {
	        		log(result)
	        	} else {
	        		log(result + ": " + button.attr(logMessage))
	        	}	        	
	        	formElements.prop('disabled', false)
	        },
	        error: function(errorThrown){
	        	error(errorThrown);
	        	formElements.prop('disabled', false)
	        }
		})
	})
	$('#container').on('click', '.reload-ajax-action', function() {
		var holder = $(this).parents(($(this).attr('holder-id') == undefined) ? '.'+ $(this).attr('holder-class') : '#' + $(this).attr('holder-id'))
		var button = $(this)
		button.prop('disabled', true)
		$.ajax({
	        url: button.attr("url"),
	        success: function(result, status, xhr) {
	        	var logMessage = button.attr('ajax-log-message')
	        	if (logMessage == undefined) {
	        		log("Update successful")
	        	} else {
	        		log(logMessage + " successful")
	        	}
	        	button.prop('disabled', false)
	        	holder.replaceWith(result);
	        },
	        error: function(errorThrown){
	        	error(errorThrown);
	        	button.prop('disabled', false)
	        }
		})
	})	
	$('#container').on('click', '.load-modal-data-ajax-action', function() {
		var modal = $($(this).attr('href'))
		$.ajax({
			url: $(this).attr('url'),
			success: function(result, status, xhr) {
				log("Received first timeline page successfully")
				var modalBody = modal.find('.modal-body')
				modalBody.html(result)
				linkifyAll(modalBody.get(), "linkify")
			},
			error: function(xhr, status, error) {
				console.log("Received error " + error) 
			}
		})
	})
})

var doNotPull = false;
function pullLog() {
	if (!doNotPull) {
		$.ajax({
	        url: '/log',
	        success:function(result, status, xhr) {
	        	for (var i = 0; i < result.length; ++i) {
	        		log(result[i].toString())
	        	}
	        },
	        error: function(errorThrown){
	        	doNotPull = true;
	        	log("Could not retrieve log from server, stop pulling logs and ratelimits. Reload to reenable.")  	
	        }
	    });
	}
}

function pullRatelimit() {
	if (!doNotPull) {
		$.ajax({
	        url: '/ratelimits',
	        success:function(result, status, xhr) {
	        	$(document).find('#ratelimits').html(result)
	        },
	        error: function(errorThrown){
	        	doNotPull = true;
	        	log("Could not retrieve ratelimit from server, stop pulling logs and ratelimits. Reload to reenable.")      	
	        }
	    });
	}
}

window.setInterval(function(){
	pullLog();
	pullRatelimit();
}, 1000);
