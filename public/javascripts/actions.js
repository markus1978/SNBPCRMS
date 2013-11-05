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
	$('#container').on('click', '.remove-empty-last-action', function() {
		var container = $('#' + $(this).attr('last-container-id'))
		var elements = container.find('.' + $(this).attr('last-class'))
		if (elements.length <= 1) {
			container.find($('#' + $(this).attr('empty-id'))).val('')
		} else {
			$('#' + $(this).attr('remove-id')).remove()
		}
	})
	$('#container').on('click', '.add-url-field-action', function() {
		var html = $($('<div></div>').html($('#' + $(this).attr('copy-id')).clone())).html();		
		var container = $('#' + $(this).attr('container-id'))
		
		html = html.replace(/\[(\d+)\]/g, function(fullMatch, n) {
			return "[" + (Number(n) + 1) + "]";
		});
		html = html.replace(/_(\d+)_/g, function(fullMatch, n) {
			return "_" + (Number(n) + 1) + "_";
		});
		container.append(html).find(":input").last().attr('value','')
		var elements = container.find('.' + $(this).attr('to-hide-class'))
		for (var i = 0; i < elements.length-1; i++) {
			$(elements[i]).attr('style', 'visibility:hidden;')
		}
		$(elements[elements.length-1]).attr('style', 'visibility:visible;')
	})	
	$('#container').on('click', '.reload-ajax-action', function() {
		var holder = $(this).parents(($(this).attr('holder-id') == undefined) ? '.'+ $(this).attr('holder-class') : '#' + $(this).attr('holder-id'))
		var button = $(this)
		button.prop('disabled', true)
		var dataId = $(this).attr('ajax-data')
		if (dataId != undefined) {
			var dataElement = holder.find('#'+dataId);
			var jsonData = {};
			$.map(dataElement.find(":input"), function(n, i) {
				var value = $(n).val();
				if ($(n).is(':checkbox')) {
					value = $(n).prop('checked')
				}
				if (jsonData[n.name] !== undefined) {
		            if (!jsonData[n.name].push) {
		            	jsonData[n.name] = [jsonData[n.name]];
		            }
		            jsonData[n.name].push(value || '');
		        } else {
		        	jsonData[n.name] = value || '';
		        }
			});			
			$.ajax({
		        url: button.attr('url'),
		        type : 'POST',
		        contentType : 'text/json',
		        data: JSON.stringify(jsonData),
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
		} else {
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
		}
	})	
	$('#container').on('click', '.load-modal-data-ajax-action', function() {
		var modal = $($(this).attr('href'))
		$.ajax({
			url: $(this).attr('url'),
			success: function(result, status, xhr) {
				log("Received first timeline page successfully")
				var modalBody = modal.find('.modal-body')
				modalBody.html(result)
				enableTooltips(modalBody)
				linkifyAll(modalBody.get(), "linkify")
			},
			error: function(xhr, status, error) {
				console.log("Received error " + error) 
			}
		})
	})
	$('#container').on('click', '.open-url-action', function() {
		var url = $('#' + $(this).attr('url-input-id')).val()
		if (!url.match('^http://')) {
			url = 'http://' + url
		}
		window.open(url,'new')
	})
})

function enableTooltips(container) {
	$(container).find('[rel="tooltip"]').each(function(index, value) {
		$(value).tooltip();
	})
}

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

function getOuterHTML(el) {   
    var wrapper = '';

    if(el)
    {
        var inner = el.innerHTML;
        var wrapper = '<' + el.tagName;

        for( var i = 0; i < el.attributes.length; i++ )
        {
            wrapper += ' ' + el.attributes[i].nodeName + '="';
            wrapper += el.attributes[i].nodeValue + '"';
        }
        wrapper += '>' + inner + '</' + el.tagName + '>';
    }
    return wrapper;
}
