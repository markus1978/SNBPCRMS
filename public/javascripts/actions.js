function log(event) {
	document.getElementById("logs").innerHTML = '<p style="margin-bottom: 0px;">[' + new Date().toLocaleString() + '] ' + event + '</p>';                
}

function error(event) {
	document.getElementById("logs").innerHTML = '<p style="margin-bottom: 0px; color: FF0000;">[' + new Date().toLocaleString() + '] ' + event + '</p>';
}


jQuery(document).ready(function($) {
	$('#importAll').click(function() {			
	    $.ajax({
	        url: '/twitter/importAll',
	        data: {
	        	query: document.getElementById('query').innerHTML
	        },
	        success:function(result, status, xhr) {
	        	log(result);
	        },
	        error: function(errorThrown){
	        	error(errorThrown);
	        }
	    });
	})
	$('.generalDataInput').change(function() {		
		var selects = $(this).parents('.general_data').find('select')
		selects.prop('disabled', true)
		var data = { id : $(this).parents('.twitterId').attr("twitterId") }
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
		});
	})
	$('.twitterUnFollow').click(function() {
		var id = $(this).parents('.twitterId').attr("twitterId");
		$(this).prop('disabled', true);
		console.log("unfollow " + id);
		$(this).prop('disabled', false);
	})
	$('.twitterFollow').click(function() {
		var id = $(this).parents('.twitterId').attr("twitterId");
		$(this).prop('disabled', true);
		console.log("follow " + id);
		$(this).prop('disabled', false);
	})
	
});


function pullLog() {
	$.ajax({
        url: '/log',
        success:function(result, status, xhr) {
        	for (var i = 0; i < result.length; ++i) {
        		log(result[i].toString())
        	}
        },
        error: function(errorThrown){
        	error(errorThrown);
        }
    });
}

window.setInterval(function(){
	pullLog();
}, 1000);
