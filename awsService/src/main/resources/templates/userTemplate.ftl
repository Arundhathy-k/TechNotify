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
            margin: 5px;
        }

        input[type="radio"], input[type="checkbox"] {
            margin-right: 5px;
        }

        .secondary-address {
            display: none;
            margin-top: 5px;
        }
        .secondary-address.visible {
            display: block;
        }

        p {
            text-align: center;
            margin-top: 10px;
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
    <form>
        <div class="form-section">
            <label>First Name: ${firstName}</label>
            <label>Last Name: ${lastName}</label>
            <label>Gender:
                <input type="radio" name="gender" value="Male" <#if gender == "Male">checked</#if>> Male
                <input type="radio" name="gender" value="Female" <#if gender == "Female">checked</#if>> Female
            </label>
            <label>Phone Number: ${phone}</label>
        </div>

        <div class="form-section">
            <h3>Primary Address</h3>
            <label>Address 1: ${primaryAddress1}</label>
            <label>Address 2: ${primaryAddress2}</label>
            <label>City: ${primaryCity}</label>
            <label>State: ${primaryState}</label>
            <label>Zipcode: ${primaryZip}</label>
        </div>

        <div class="form-section">
            <h3>Secondary Address</h3>
            <label>
                <input type="checkbox" name="secondary" <#if secondaryAddress1?has_content>checked</#if> onchange="toggleSecondaryAddress(this)">
                Enable Secondary Address
            </label>
            <div class="secondary-address <#if secondaryAddress1?has_content>visible</#if>">
                <label>Address 1: ${secondaryAddress1}</label>
                <label>Address 2: ${secondaryAddress2}</label>
                <label>City: ${secondaryCity}</label>
                <label>State: ${secondaryState}</label>
                <label>Zipcode: ${secondaryZip}</label>
            </div>
        </div>

        <div class="form-section">
            <h3>Company Details</h3>
            <label>Company Name: ${companyName}</label>
            <label>Location: ${companyLocation}</label>
            <label>Designation: ${companyDesignation}</label>
            <label>Date of Joining: ${dateOfJoining}</label>
            <label>Experience: ${experience} years</label>
        </div>

        <div class="form-section">
            <label>
                <input type="checkbox" name="agree" checked="checked">
                I agree all the details are true. If any false statement is provided, you can take any action against me.
            </label>
        </div>
    </form>
    <p>Yours truly,</p>
    <p>
        ${firstName}, ${.now?string("yyyy-MM-dd HH:mm:ss")}
    </p>
</body>
</html>

