<!DOCTYPE html>
<html>
<head>
    <title>User Details</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            color: #333;
        }

        h1 {
            text-align: center;
            color: #444;
        }

        label {
            display: block;
            margin: 10px 0;

        }

        input[type="text"], input[type="email"], input[type="checkbox"] {
            margin: 5px 0;
            padding: 8px;
            font-size: 14px;
            width: 100%;
            max-width: 400px;
        }

        input[type="checkbox"] {
            width: auto;
        }

        input[type="radio"] {
            width: auto;
            margin-right: 5px;
        }

        .secondary-address {
            display: none;
            margin-top: 20px;
            padding: 10px;
            background-color: #fff;
        }

        button {
            display: block;
            margin: 20px auto;
            padding: 10px 20px;
            font-size: 16px;
            color: #fff;
            background-color: #007BFF;
            cursor: pointer;
        }

        .checkbox-label {
            font-weight: normal;
            margin-left: 5px;
        }

        p {
            text-align: center;
            margin-top: 20px;
        }
    </style>
    <script>
        function toggleSecondaryAddress(checkbox) {
            const secondarySection = document.querySelector('.secondary-address');
            secondarySection.style.display = checkbox.checked ? 'block' : 'none';
        }
    </script>
</head>
<body>
    <h1><u>User Details</u></h1>

    <div class="form-section">
        <label>User ID: <span>[[${userId}]]</span></label>
        <label>Name: <span>[[${name}]]</span></label>
        Gender <label>
            <input type="radio" name="gender"> MALE</input>
        </label>
        <label>
            <input type="radio" name="gender"> FEMALE</input>
        </label>
        <label>Email: <span>[[${email}]]</span></label>
        <label>Phone Number: <span>[[${phone}]]</span></label>
    </div>

    <div class="form-section">
        <h3>Primary Address</h3>
        <label>Address: <span>[[${address}]]</span></label>
    </div>

    <div class="form-section">
        <h3>Secondary Address</h3>
        <label>
            <input type="checkbox" name="secondary" onchange="toggleSecondaryAddress(this)">
            Enable Secondary Address</input>
        </label>
        <div class="secondary-address">
            <label>Address 1: <input type="text" placeholder="Enter address 1" /></label>
            <label>Address 2: <input type="text" placeholder="Enter address 2" /></label>
            <label>City: <input type="text" placeholder="Enter city" /></label>
            <label>State: <input type="text" placeholder="Enter state" /></label>
            <label>Zipcode: <input type="text" placeholder="Enter Zipcode" /></label>
        </div>
    </div>

    <div class="form-section">
        <label>
            <input type="checkbox" name="agree">
            I agree all the details are true. If any false statement is provided, you can take any action against me.</input>
        </label>
    </div>

    <p>Yours truly,</p>
    <p>[[${name}]]</p>
</body>
</html>