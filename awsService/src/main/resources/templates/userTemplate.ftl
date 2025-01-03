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
           <pre><label>First Name  : ${firstName}   Last Name   : ${lastName}</label></pre>
           <pre><label>Gender      : <input type="radio" name="gender" value="Male" <#if gender == "Male">checked</#if>> Male  <input type="radio" name="gender" value="Female" <#if gender == "Female">checked</#if>> Female</pre>
            </label>
           <pre><label>Phone Number: ${phone}</label></pre>
        </div>

        <div class="form-section">
            <h3>Primary Address</h3>
            <pre><label>Address 1: ${primaryAddress1}</label></pre>
            <pre><label>Address 2: ${primaryAddress2}</label></pre>
            <pre><label>City     : ${primaryCity}</label></pre>
            <pre><label>State    : ${primaryState}</label></pre>
            <pre><label>Zipcode  : ${primaryZip}</label></pre>
        </div>

        <div class="form-section">
            <h3>Secondary Address</h3>
            <label>
                <input type="checkbox" name="secondary" <#if secondaryAddress1?has_content>checked</#if> onchange="toggleSecondaryAddress(this)">
                Enable Secondary Address
            </label>
            <div class="secondary-address <#if secondaryAddress1?has_content>visible</#if>">
              <pre><label>Address 1: ${secondaryAddress1}</label></pre>
              <pre><label>Address 2: ${secondaryAddress2}</label></pre>
              <pre><label>City     : ${secondaryCity}</label></pre>
              <pre><label>State    : ${secondaryState}</label></pre>
              <pre><label>Zipcode  : ${secondaryZip}</label></pre>
            </div>
        </div>

        <div class="form-section">
            <h3>Company Details</h3>
          <pre><label>Company Name   : ${companyName}</label></pre>
          <pre><label>Location       : ${companyLocation}</label></pre>
          <pre><label>Designation    : ${companyDesignation}</label></pre>
          <pre><label>Date of Joining: ${dateOfJoining}</label></pre>
          <pre><label>Experience     : ${experience} years</label></pre>
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
