var args = JSON.parse(arguments[0]) || {
    message: "Hello world!",
};

if (args.message === "error") {
    return { statusCode: 404 };
}

for (var i = 0; i < 40; i++) {
    var e = document.createElement("span");
    e.textContent = args.message;
    e.style.position = "absolute";
    e.style.top = (i * 11) + "px";
    e.style.left = (i * 14) + "px";
    document.body.appendChild(e);
}

return {
    targetElement: document.getElementById("target"),
    httpCache: {
        maxAge: 8 * 60 * 60
    },
};
