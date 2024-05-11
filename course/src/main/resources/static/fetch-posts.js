function fetchPosts(currentUrl) {
  console.log("do request");
  fetch(currentUrl, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  })
    .then((response) => response.json())
    .then((data) => {
      // Get the container where posts will be displayed
      const container = document.getElementById("postsContainer");

      // Iterate through each post in the JSON data
      data.forEach((post) => {
        // Create a div element to hold each post
        const postElement = document.createElement("div");
        // Set the content of the post element
        postElement.innerHTML = `
            <h3>${post.content}</h3>
            <p>Posted by: ${post.userId}</p>
            <hr>
        `;

        console.log(post);

        // Append the post element to the container
        container.appendChild(postElement);
      });
    });
}
