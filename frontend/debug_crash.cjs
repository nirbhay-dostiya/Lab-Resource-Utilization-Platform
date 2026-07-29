const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  // Listen for console logs and errors
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('BROWSER ERROR:', msg.text());
    }
  });
  
  page.on('pageerror', err => {
    console.log('PAGE EXCEPTION:', err.toString());
  });

  try {
    // First need to login because Dashboard is protected
    await page.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' });
    
    // Check if login form exists
    const hasLogin = await page.$('input[type="email"]');
    if (hasLogin) {
      await page.type('input[type="email"]', 'inst_admin1@test.com');
      await page.type('input[type="password"]', 'password123');
      await Promise.all([
        page.click('button[type="submit"]'),
        page.waitForNavigation({ waitUntil: 'networkidle0' })
      ]);
    } else {
      await page.goto('http://localhost:5173/dashboard', { waitUntil: 'networkidle0' });
    }

    console.log('On Dashboard');
    
    // Find the Billing button
    // It's a button or div with text 'Billing & Invoices'
    const elements = await page.$$x("//*[contains(text(), 'Billing & Invoices')]");
    if (elements.length > 0) {
      console.log('Clicking Billing button');
      await elements[0].click();
      
      // Wait to see if crash occurs
      await new Promise(r => setTimeout(r, 2000));
    } else {
      console.log('Billing button not found!');
    }
  } catch (err) {
    console.log('SCRIPT ERROR:', err.message);
  } finally {
    await browser.close();
  }
})();
