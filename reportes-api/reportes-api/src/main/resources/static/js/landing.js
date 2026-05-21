
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function(e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({ behavior: 'smooth' });
        }
    });
});

window.addEventListener('scroll', () => {
    const nav = document.querySelector('nav');
    if (window.scrollY > 50) {
        nav.style.boxShadow = '0 4px 30px rgba(142,68,173,0.2)';
    } else {
        nav.style.boxShadow = '0 2px 20px rgba(142,68,173,0.1)';
    }
});

const stats = document.querySelectorAll('.stat-number');
const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const target = parseInt(entry.target.textContent);
            let count = 0;
            const step = target / 40;
            const timer = setInterval(() => {
                count += step;
                if (count >= target) {
                    entry.target.textContent = target;
                    clearInterval(timer);
                } else {
                    entry.target.textContent = Math.floor(count);
                }
            }, 30);
            observer.unobserve(entry.target);
        }
    });
});

stats.forEach(stat => observer.observe(stat));
const cards = document.querySelectorAll('.feature-card, .role-card, .team-card');
const cardObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry, index) => {
        if (entry.isIntersecting) {
            setTimeout(() => {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }, index * 100);
            cardObserver.unobserve(entry.target);
        }
    });
}, { threshold: 0.1 });

cards.forEach(card => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(20px)';
    card.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
    cardObserver.observe(card);
});