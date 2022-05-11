//$(document).ready(function() {

		function checkConnection() {
			var s = document.forms["connform"]["server"].value;
			var p = document.forms["connform"]["port"].value;
			var ep = document.forms["connform"]["endpoint"].value;
			var user = document.forms["connform"]["userID"].value;
			var topic = document.forms["connform"]["topic"].value;
			//usa l'url specificato in back-end per richiedere la connessione al server sul giusto canale
			var url = "http://"+s+":"+p+"/"+ep+"?userID="+user+"&topic="+topic; 
			//tramite l'oggetto EventSource si mette in ascolto di event Emitters che gli faranno pervenire gli eventi
			var eventSource = new EventSource(url); 
			
			/* //non fa visualizzare in console il messaggio
			eventSource.onMessage = function(evt) {
				console.log(evt.data);
			}*/

			//'open' e 'error' sono evnti predefiniti, non vanno definiti nel back-end
			eventSource.addEventListener("open", (event) => {
				console.log('connection is live');
				$("#status").html("Connected");
				$("#status").css("background-color", "green");
			});

			//gestisce gli eventi dell'emitter chiamati 'latestNews' sulla parte di Back-End, il secondo parametro è una funzione di call-back 
			//che prende e gestisce il messaggio arrivato dall'evento del server, questo mi permette di poter gestire diversamente eventi di tipo diverso arrivati dal server
			eventSource.addEventListener("latestNews", function(event){
				console.log(event.lastEventId);
				var articleData = JSON.parse(event.data);
				addBlock(articleData.title, articleData.text);
			});

			//‘error’ event will be called whenever there is a network error and also when the server closes the connection by calling a 'complete’ or ‘completeWithError’ method on the emitter.
			eventSource.addEventListener("error", function(event){
				console.log("Error: " + event.currentTarget.readyState);
				if (event.currentTarget.readyState == EventSource.CLOSED) {
					
				}
				else{
					$("#status").html("Disconnected");
					$("#status").css("background-color", "red");
					eventSource.close();
				}
			});

			return false;
		}
		
		
	//});

	window.onBeforeunload = function() {
		eventSource.close();
	}

	function addBlock(title,text) {
		var a = document.createElement('article');
		var h = document.createElement('h3');
		var t = document.createTextNode(title);
		h.appendChild(t);
		var para = document.createElement('p');
		para.innerHTML = text;
		a.appendChild(h);
		a.appendChild(para);
		document.getElementById('pack').appendChild(a);
	}

	const unsubscribeTopic = async () => {
		var user = document.forms["connform"]["userID"].value;
		var topic = document.forms["connform"]["topic"].value;

		const response = await fetch('http://localhost:6033/unsubscribe', {
		    method: 'POST',
		    body: 
		    '{user: ' + user + ', topic : ' + topic + '}', //TODO stringify the object
		    headers: {
		      'Content-Type': 'application/json'
		    }
		  });
	  	//const myJson = await response.json(); //extract JSON from the http response
	  	// do something with myJson
	  	//console.log(myJson);
	}