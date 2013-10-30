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
	$('.simple-ajax-action').click(function() {			
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
	$('#twitter-user-holders').on('change', '.general-data-input', function() {		
		var selects = $(this).parents('.general-data').find('select')
		selects.prop('disabled', true)
		var data = { id : $(this).parents('.twitter-id').attr("twitterId") }
		data[this.name] = this.value
		$.ajax({
	        url: '/twitter/update',
	        type : 'POST',
	        contentType : 'text/json',
	        data: JSON.stringify(data),
	        success: function(result, status, xhr) {
	        	log(result)
	        	selects.prop('disabled', false)
	        },
	        error: function(errorThrown){
	        	error(errorThrown);
	        	selects.prop('disabled', false)
	        }
		})
	})
	$('#twitter-user-holders').on('click', '.reload-ajax-action', function() {
		var twitterDataHolder = $(this).parents('.twitter-data-holder')
		var button = $(this)
		button.prop('disabled', true)
		$.ajax({
	        url: button.attr("url"),
	        success: function(result, status, xhr) {
	        	log(button.attr('message') + " successful.")
	        	button.prop('disabled', false)
	        	twitterDataHolder.replaceWith(result);
	        },
	        error: function(errorThrown){
	        	error(errorThrown);
	        	button.prop('disabled', false)
	        }
		})
	})	
	$('#twitter-user-holders').on('click', '.load-modal-data-ajax-action', function() {
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

var doNotPullLogs = false;
function pullLog() {
	if (!doNotPullLogs) {
		$.ajax({
	        url: '/log',
	        success:function(result, status, xhr) {
	        	for (var i = 0; i < result.length; ++i) {
	        		log(result[i].toString())
	        	}
	        },
	        error: function(errorThrown){
	        	doNotPullLogs = true;
	        	log("Could not retrieve log from server, stop pulling logs. Reload to reenable logs.")
	        	error(errorThrown);        	
	        }
	    });
	}
}

window.setInterval(function(){
	pullLog();
}, 1000);
