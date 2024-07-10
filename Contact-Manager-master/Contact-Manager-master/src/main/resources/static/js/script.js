const toggleSidebar = () => {
	if ($(".sidebar").is(":visible")) {
		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "0%");
	}
	else {
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "20%");
	}
};

function deleteContact(cid) {
	swal({
		title: "Are you sure?",
		text: "You want to delete this contect...",
		icon: "warning",
		buttons: true,
		dangerMode: true,
	})
		.then((willDelete) => {
			if (willDelete) {
				window.location = "/user/delete/" + cid;
			} else {
				swal("Your contact is safe !!");
			}
		});
}

const search = () => {
	//console.log("searching....");

	let query = $("#search-input").val();

	if (query == "") {
		$(".search-result").hide();
	}
	else {
		//search
		console.log(query);

		//sending request to the server

		let url = `http://localhost:8080/search/${query}`;

		fetch(url)
			.then((response) => {
				return response.json();
			})
			.then((data) => {
				//data.......
				console.log(data);

				let text = `<div class='list-group'>`;

				data.forEach((contact) => {
					text += `<a href='/user/contact/${contact.cId}' class='list-group-item list-group-item-action'> ${contact.name} </a>`;
				});

				text += `</div>`;

				$(".search-result").html(text);
				$(".search-result").show();
			});

	}
};


//request to server to cerate order

const paymentStart = () => {
	var amount = $("#payment_field").val();

	if (amount == "" || amount == null) {
		swal("Failed !!", "Please Enter Amount !!", "error");
		return;
	}

	//send request to the server for payment using ajax

	$.ajax(
		{
			url: '/user/create_order',
			data: JSON.stringify({ amount: amount, info: 'order_request' }),
			contentType: 'application/json',
			type: 'POST',
			dataType: 'json',

			success: function(response) {
				//invoked when success
				console.log(response);
				if (response.status == 'created') {
					//open payment form
					var options = {
						key: "rzp_test_pHJMo2dA8t3dbJ",
						amount: response.amount,
						currency: "INR",
						name: "Smart Contact Manager",
						description: "Donation",
						image: "https://static.vecteezy.com/system/resources/previews/009/022/581/original/scm-logo-scm-letter-scm-letter-logo-design-initials-scm-logo-linked-with-circle-and-uppercase-monogram-logo-scm-typography-for-technology-business-and-real-estate-brand-vector.jpg",
						order_id: response.id,

						handler: function(response) {
							console.log(response.razorpay_payment_id);
							console.log(response.razorpay_order_id);
							console.log(response.razorpay_signature);
							//console.log("Payment SuccessFul !!");

							updatePaymentOnServer(
								response.razorpay_payment_id,
								response.razorpay_order_id,
								"Paid"
							);


						},
						prefill: {
							name: "",
							email: "",
							contact: ""
						},
						notes: {
							"address": "Smart Contact Manager",
						},
						theme: {
							"color": "#3399cc"
						}
					};

					//payment to initiat karna hai

					var rzp = new Razorpay(options);
					rzp.on('payment.failed', function(response) {
						console.log(response.error.code);
						console.log(response.error.description);
						console.log(response.error.source);
						console.log(response.error.step);
						console.log(response.error.reason);
						console.log(response.error.metadata.order_id);
						console.log(response.error.metadata.payment_id);
						swal("Failed !!", "Something went wrong !!", "error");
					});

					rzp.open();




				}
			},
			error: function(response) {
				//when error
				console.log(error)
				alert("something went wrong")
			}


		}

	)


};

function updatePaymentOnServer(payement_id, order_id, status)
{

	$.ajax({

		url: '/user/update_order',
		data: JSON.stringify({
			payment_id: payement_id,
			order_id: order_id, status: status,
		}),
		contentType: 'application/json',
		type: 'POST',
		dataType: 'json',

		success: function(response) {
			swal("Thank you !!", "Congrates !! Payment SuccessFul", "success");
		},
		error: function(error) {
			swal("Thank you !!", "Congrates !!! Payment SuccessFul", "success");
		}
	});
}









