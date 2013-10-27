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
		var generalDataElement = $(this).parent().parent().parent() 
		generalDataElement.attr("style", "background:#FF0000;");
		var data = { id : generalDataElement.find("input[name='id']").attr("value") }
		data[this.name] = this.value
		$.ajax({
	        url: '/twitter/update',
	        type : 'POST',
	        contentType : 'text/json',
	        data: JSON.stringify(data),
	        success: function(result, status, xhr) {
	        	log(result)
	        	generalDataElement.attr("style", "");
	        },
	        error: function(errorThrown){
	        	error(errorThrown);
	        	generalDataElement.attr("style", "");
	        }
		});
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
