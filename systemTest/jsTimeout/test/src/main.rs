extern crate reqwest;

use std::io;
use std::time;
use std::thread;

fn create_http_client() -> reqwest::Result<reqwest::Client> {
    return reqwest::Client::builder().timeout(time::Duration::from_secs(100)).build();
}

fn request_no_response_endpoint() {
    println!("Start request");
    let now = time::Instant::now();
    let mut res = create_http_client().unwrap().get("http://localhost:8080/no-response").send().unwrap();
    res.copy_to(&mut io::stdout()).unwrap();
    println!("  elapsed : {} s", now.elapsed().as_secs());
}

fn request_health_check() {
    println!("Start health check");
    let now = time::Instant::now();
    let mut res = create_http_client().unwrap().get("http://localhost:8080/-/health/all").send().unwrap();
    res.copy_to(&mut io::stdout()).unwrap();
    println!("  elapsed : {} s", now.elapsed().as_secs());
}

fn main() {
    let mut h = vec![];

    h.push(thread::spawn(|| { request_no_response_endpoint() }));
    thread::sleep(time::Duration::from_millis(5));
    h.push(thread::spawn(|| { request_no_response_endpoint() }));
    thread::sleep(time::Duration::from_millis(5));
    h.push(thread::spawn(|| { request_no_response_endpoint() }));
    thread::sleep(time::Duration::from_millis(5));
    h.push(thread::spawn(|| { request_no_response_endpoint() }));
    thread::sleep(time::Duration::from_millis(5));
    h.push(thread::spawn(|| { request_no_response_endpoint() }));
    thread::sleep(time::Duration::from_millis(5));
    h.push(thread::spawn(|| { request_no_response_endpoint() }));
    thread::sleep(time::Duration::from_millis(5));
    h.push(thread::spawn(|| { request_health_check() }));

    for handle in h {
        let _ = handle.join();
    }
}
